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

package tech.pegasys.teku.beacon.sync.historical;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.dishonestPayloadRoot;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.honestPayloadRoot;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.tamperPayload;

import java.util.Optional;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networking.eth2.peers.RespondingEth2Peer;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.gloas.BeaconBlockBodyGloas;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadBid;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.SignedExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.generator.ChainBuilder;
import tech.pegasys.teku.spec.logic.common.util.AsyncBLSSignatureVerifier;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;
import tech.pegasys.teku.statetransition.blobs.BlobSidecarManager;
import tech.pegasys.teku.storage.api.StorageQueryChannel;
import tech.pegasys.teku.storage.api.StorageUpdateChannel;
import tech.pegasys.teku.storage.client.CombinedChainDataClient;
import tech.pegasys.teku.storage.storageSystem.InMemoryStorageSystemBuilder;
import tech.pegasys.teku.storage.storageSystem.StorageSystem;

class A1BackfillPairingTest {

  private static final UInt64 LAST_GLOAS_SLOT = UInt64.valueOf(7);
  private static final UInt64 FIRST_HEZE_SLOT = UInt64.valueOf(8);

  private final Spec spec = TestSpecFactory.createMinimalWithHezeForkEpoch(UInt64.ONE);
  private final ChainBuilder chainBuilder = ChainBuilder.create(spec);

  A1BackfillPairingTest() {
    chainBuilder.generateGenesis();
    chainBuilder.generateBlocksUpToSlot(10);
  }

  @Test
  void a1GloasRejectsUncomputedBlockHash() {
    final SignedExecutionPayloadEnvelope tampered = signed(tamperPayload(at(LAST_GLOAS_SLOT)));

    assertThatThrownBy(() -> fetcher().processExecutionPayload(tampered))
        .hasMessageContaining("block hash");
  }

  @Test
  void a1HezeAcceptsHonestPayloadRoot() {
    final SignedExecutionPayloadEnvelope honest = signed(at(FIRST_HEZE_SLOT));

    assertThatCode(() -> backfill(honestPayloadRoot(honest.getMessage()), honest))
        .doesNotThrowAnyException();
  }

  @Test
  void a1HezeRejectsDishonestPayloadRoot() {
    final SignedExecutionPayloadEnvelope honest = signed(at(FIRST_HEZE_SLOT));

    assertThatThrownBy(() -> backfill(dishonestPayloadRoot(honest.getMessage()), honest))
        .hasMessageContaining("payload root");
  }

  @Test
  void a1HezeRejectsTamperedPayloadUnderHonestRoot() {
    final ExecutionPayloadEnvelope honest = at(FIRST_HEZE_SLOT);
    final SignedExecutionPayloadEnvelope tampered = signed(tamperPayload(honest));

    assertThatThrownBy(() -> backfill(honestPayloadRoot(honest), tampered))
        .hasMessageContaining("payload root");
  }

  private void backfill(
      final Bytes32 bidPayloadRoot, final SignedExecutionPayloadEnvelope envelope) {
    final HistoricalBatchFetcher fetcher = fetcher();
    fetcher.processExecutionPayload(envelope);
    fetcher.validateExecutionPayloadEnvelope(
        envelope.blind(schemas(envelope)),
        blockWithBidCarrying(envelope.getMessage().getSlot(), bidPayloadRoot));
  }

  private SignedBeaconBlock blockWithBidCarrying(final UInt64 slot, final Bytes32 payloadRoot) {
    final SignedBeaconBlock real = chainBuilder.getBlockAtSlot(slot);
    final ExecutionPayloadBid bid =
        real.getMessage()
            .getBody()
            .toVersionGloas()
            .orElseThrow()
            .getSignedExecutionPayloadBid()
            .getMessage()
            .withPayloadRoot(payloadRoot);
    final SignedBeaconBlock block = mock(SignedBeaconBlock.class, RETURNS_DEEP_STUBS);
    when(block.getSlot()).thenReturn(real.getSlot());
    when(block.getRoot()).thenReturn(real.getRoot());
    final BeaconBlockBodyGloas body = mock(BeaconBlockBodyGloas.class, RETURNS_DEEP_STUBS);
    when(body.getSignedExecutionPayloadBid().getMessage()).thenReturn(bid);
    when(block.getMessage().getBody().toVersionGloas()).thenReturn(Optional.of(body));
    return block;
  }

  private ExecutionPayloadEnvelope at(final UInt64 slot) {
    return chainBuilder.getExecutionPayloadAtSlot(slot).orElseThrow().getMessage();
  }

  private SignedExecutionPayloadEnvelope signed(final ExecutionPayloadEnvelope envelope) {
    return SchemaDefinitionsGloas.required(spec.atSlot(envelope.getSlot()).getSchemaDefinitions())
        .getSignedExecutionPayloadEnvelopeSchema()
        .create(
            envelope,
            chainBuilder
                .getExecutionPayloadAtSlot(envelope.getSlot())
                .orElseThrow()
                .getSignature());
  }

  private SchemaDefinitionsGloas schemas(final SignedExecutionPayloadEnvelope envelope) {
    return SchemaDefinitionsGloas.required(
        spec.atSlot(envelope.getMessage().getSlot()).getSchemaDefinitions());
  }

  private HistoricalBatchFetcher fetcher() {
    final StorageSystem storageSystem =
        InMemoryStorageSystemBuilder.create().specProvider(spec).build();
    storageSystem.chainUpdater().initializeGenesis();
    final SignedBeaconBlock last = chainBuilder.getLatestBlockAndState().getBlock();
    return new HistoricalBatchFetcher(
        mock(StorageUpdateChannel.class),
        mock(AsyncBLSSignatureVerifier.class),
        new CombinedChainDataClient(
            storageSystem.recentChainData(), mock(StorageQueryChannel.class), spec),
        spec,
        mock(BlobSidecarManager.class),
        RespondingEth2Peer.create(spec, chainBuilder),
        last.getSlot(),
        last.getRoot(),
        UInt64.valueOf(10),
        Optional.empty(),
        5);
  }
}
