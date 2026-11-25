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

package tech.pegasys.teku.dataproviders.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.honestPayloadRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.ssz.collections.impl.SszByteListImpl;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.SignedExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadBlockHashCalculator;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadBody;
import tech.pegasys.teku.spec.datastructures.execution.versions.capella.ExecutionPayloadCapella;
import tech.pegasys.teku.spec.datastructures.execution.versions.gloas.ExecutionPayloadGloas;
import tech.pegasys.teku.spec.generator.ChainBuilder;

class A3EnvelopeUnblindingTest {

  private static final UInt64 LAST_GLOAS_SLOT = UInt64.valueOf(7);
  private static final UInt64 FIRST_HEZE_SLOT = UInt64.valueOf(8);

  private final Spec spec = TestSpecFactory.createMinimalWithHezeForkEpoch(UInt64.ONE);
  private final ChainBuilder chainBuilder = ChainBuilder.create(spec);

  A3EnvelopeUnblindingTest() {
    chainBuilder.generateGenesis();
    chainBuilder.generateBlocksUpToSlot(10);
  }

  @Test
  void a3GloasRebuildKeepsBlockHash() {
    final SignedExecutionPayloadEnvelope stored = stored(LAST_GLOAS_SLOT);

    final ExecutionPayloadEnvelope rebuilt =
        unblind(stored, body(stored.getMessage().getPayload())).orElseThrow();

    assertThat(
            ExecutionPayloadBlockHashCalculator.computeGloasBlockHash(
                rebuilt, spec.getExecutionRequestsDataCodec(LAST_GLOAS_SLOT)))
        .isEqualTo(stored.getMessage().getPayload().getBlockHash());
  }

  @Test
  void a3HezeRebuildKeepsPayloadRoot() {
    final SignedExecutionPayloadEnvelope stored = stored(FIRST_HEZE_SLOT);

    final ExecutionPayloadEnvelope rebuilt =
        unblind(stored, body(stored.getMessage().getPayload())).orElseThrow();

    assertThat(honestPayloadRoot(rebuilt)).isEqualTo(honestPayloadRoot(stored.getMessage()));
  }

  @Test
  void a3GloasRejectsMismatchedBody() {
    final SignedExecutionPayloadEnvelope stored = stored(LAST_GLOAS_SLOT);

    assertThat(unblind(stored, extraTransaction(body(stored.getMessage().getPayload())))).isEmpty();
  }

  @Test
  void a3HezeRejectsMismatchedBody() {
    final SignedExecutionPayloadEnvelope stored = stored(FIRST_HEZE_SLOT);

    assertThat(unblind(stored, extraTransaction(body(stored.getMessage().getPayload())))).isEmpty();
  }

  private SignedExecutionPayloadEnvelope stored(final UInt64 slot) {
    return chainBuilder.getExecutionPayloadAtSlot(slot).orElseThrow();
  }

  private Optional<ExecutionPayloadEnvelope> unblind(
      final SignedExecutionPayloadEnvelope stored, final ExecutionPayloadBody body) {
    final Bytes32 root = stored.getBeaconBlockRoot();
    final UnblindingExecutionPayloadProvider provider =
        new UnblindingExecutionPayloadProvider(
            spec,
            roots -> SafeFuture.completedFuture(Map.of(root, stored.blind(spec))),
            hashes -> SafeFuture.completedFuture(List.of(body)));
    return Optional.ofNullable(provider.getExecutionPayloads(Set.of(root)).join().get(root))
        .map(SignedExecutionPayloadEnvelope::getMessage);
  }

  private static ExecutionPayloadBody body(final ExecutionPayload payload) {
    return new ExecutionPayloadBody(
        payload.getTransactions().stream().map(SszByteListImpl::getBytes).toList(),
        ExecutionPayloadCapella.required(payload).getWithdrawals().stream()
            .map(
                w ->
                    new ExecutionPayloadBody.EncodedWithdrawal(
                        w.getIndex(), w.getValidatorIndex(), w.getAddress(), w.getAmount()))
            .toList(),
        ExecutionPayloadGloas.required(payload).getBlockAccessList().getBytes());
  }

  private static ExecutionPayloadBody extraTransaction(final ExecutionPayloadBody body) {
    final List<Bytes> transactions = new ArrayList<>(body.transactions());
    transactions.add(Bytes.of(0x02));
    return new ExecutionPayloadBody(transactions, body.withdrawals(), body.blockAccessList());
  }
}
