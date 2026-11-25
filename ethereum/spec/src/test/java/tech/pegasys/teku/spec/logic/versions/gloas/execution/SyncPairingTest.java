/*
 * Copyright (c) 2026 Consensys Incorporated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.spec.logic.versions.gloas.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.tamperPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.bls.BLSSignatureVerifier;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.SignedExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.state.AnchorPoint;
import tech.pegasys.teku.spec.generator.ChainBuilder;
import tech.pegasys.teku.spec.logic.common.execution.ExecutionPayloadVerificationException;
import tech.pegasys.teku.spec.logic.versions.bellatrix.block.OptimisticExecutionPayloadExecutor;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;

class SyncPairingTest {

  private static final UInt64 LAST_GLOAS_SLOT = UInt64.valueOf(7);
  private static final UInt64 FIRST_HEZE_SLOT = UInt64.valueOf(8);

  private final Spec spec = TestSpecFactory.createMinimalWithHezeForkEpoch(UInt64.ONE);
  private final ChainBuilder chainBuilder = ChainBuilder.create(spec);
  private final BLSSignatureVerifier rejectAll = mock(BLSSignatureVerifier.class);
  private final List<ExecutionPayload> sentToEl = new ArrayList<>();
  private final OptimisticExecutionPayloadExecutor el =
      (header, request) -> sentToEl.add(request.getExecutionPayload());

  SyncPairingTest() {
    chainBuilder.generateGenesis();
    chainBuilder.generateBlocksUpToSlot(10);
    when(rejectAll.verify(any(BLSPublicKey.class), any(), any(BLSSignature.class)))
        .thenReturn(false);
  }

  @Test
  void g3GloasHandsTamperedPayloadToElUntouched() throws Exception {
    final ExecutionPayloadEnvelope tampered = tamperPayload(at(LAST_GLOAS_SLOT));

    verify(signed(tampered), BLSSignatureVerifier.NOOP);

    assertThat(sentToEl).containsExactly(tampered.getPayload());
  }

  @Test
  void g4GloasStillChecksSignature() {
    assertThatThrownBy(() -> verify(signed(at(LAST_GLOAS_SLOT)), rejectAll))
        .isInstanceOf(ExecutionPayloadVerificationException.class)
        .hasMessageContaining("Signature");
  }

  @Test
  void g6GloasAnchorHandsTamperedPayloadToElUntouched() throws Exception {
    final ExecutionPayloadEnvelope tampered = tamperPayload(at(LAST_GLOAS_SLOT));

    verifyAsAnchor(signed(tampered), BLSSignatureVerifier.NOOP);

    assertThat(sentToEl).containsExactly(tampered.getPayload());
  }

  @Test
  void h2HezeSyncAcceptsHonestUnsignedEnvelope() throws Exception {
    final ExecutionPayloadEnvelope honest = at(FIRST_HEZE_SLOT);

    verify(unsigned(honest), rejectAll);

    assertThat(sentToEl).containsExactly(honest.getPayload());
  }

  @Test
  void h2HezeSyncRejectsTamperedPayloadUnderHonestRoot() {
    assertThatThrownBy(() -> verify(unsigned(tamperPayload(at(FIRST_HEZE_SLOT))), rejectAll))
        .isInstanceOf(ExecutionPayloadVerificationException.class)
        .hasMessageContaining("payload root");
    assertThat(sentToEl).isEmpty();
  }

  @Test
  void h2HezeAnchorRejectsTamperedPayloadUnderHonestRoot() {
    assertThatThrownBy(
            () -> verifyAsAnchor(unsigned(tamperPayload(at(FIRST_HEZE_SLOT))), rejectAll))
        .isInstanceOf(ExecutionPayloadVerificationException.class)
        .hasMessageContaining("payload root");
  }

  @Test
  void f2SpanValidatesEachEnvelopeUnderItsOwnFork() {
    final SignedExecutionPayloadEnvelope lastGloas = signed(tamperPayload(at(LAST_GLOAS_SLOT)));
    final SignedExecutionPayloadEnvelope firstHeze = unsigned(tamperPayload(at(FIRST_HEZE_SLOT)));

    assertThatCode(() -> verify(lastGloas, BLSSignatureVerifier.NOOP)).doesNotThrowAnyException();
    assertThatThrownBy(() -> verify(firstHeze, BLSSignatureVerifier.NOOP))
        .isInstanceOf(ExecutionPayloadVerificationException.class)
        .hasMessageContaining("payload root");
  }

  private void verify(
      final SignedExecutionPayloadEnvelope envelope, final BLSSignatureVerifier signatures)
      throws ExecutionPayloadVerificationException {
    spec.getExecutionPayloadVerifier(envelope.getSlot())
        .verifyExecutionPayloadEnvelope(
            envelope, chainBuilder.getStateAtSlot(envelope.getSlot()), signatures, Optional.of(el));
  }

  private void verifyAsAnchor(
      final SignedExecutionPayloadEnvelope envelope, final BLSSignatureVerifier signatures)
      throws ExecutionPayloadVerificationException {
    spec.getExecutionPayloadVerifier(envelope.getSlot())
        .verifyCheckpointAnchorExecutionPayloadEnvelope(
            envelope,
            AnchorPoint.fromInitialState(spec, chainBuilder.getStateAtSlot(envelope.getSlot())),
            signatures,
            Optional.of(el));
  }

  private ExecutionPayloadEnvelope at(final UInt64 slot) {
    return chainBuilder.getExecutionPayloadAtSlot(slot).orElseThrow().getMessage();
  }

  private SignedExecutionPayloadEnvelope signed(final ExecutionPayloadEnvelope envelope) {
    return wrap(
        envelope,
        chainBuilder.getExecutionPayloadAtSlot(envelope.getSlot()).orElseThrow().getSignature());
  }

  private SignedExecutionPayloadEnvelope unsigned(final ExecutionPayloadEnvelope envelope) {
    return wrap(envelope, BLSSignature.empty());
  }

  private SignedExecutionPayloadEnvelope wrap(
      final ExecutionPayloadEnvelope envelope, final BLSSignature signature) {
    return SchemaDefinitionsGloas.required(spec.atSlot(envelope.getSlot()).getSchemaDefinitions())
        .getSignedExecutionPayloadEnvelopeSchema()
        .create(envelope, signature);
  }
}
