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

package tech.pegasys.teku.networking.eth2.rpc.core.encodings.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.SignedExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;
import tech.pegasys.teku.spec.util.DataStructureUtil;
import tech.pegasys.teku.storage.client.RecentChainData;

class A4EnvelopeServingTest {

  private static final Bytes4 GLOAS_DIGEST = Bytes4.fromHexStringLenient("0x02");
  private static final Bytes4 HEZE_DIGEST = Bytes4.fromHexStringLenient("0x03");

  private final Spec spec = TestSpecFactory.createMinimalWithHezeForkEpoch(UInt64.ONE);
  private final DataStructureUtil dataStructureUtil = new DataStructureUtil(spec);
  private final RecentChainData recentChainData = mock(RecentChainData.class);
  private final ForkDigestRpcContextCodec<SignedExecutionPayloadEnvelope> codec =
      new ForkDigestRpcContextCodec<>(
          spec, recentChainData, ForkDigestPayloadContext.SIGNED_EXECUTION_PAYLOAD_ENVELOPE);

  @BeforeEach
  void setUp() {
    when(recentChainData.getForkDigest(eq(SpecMilestone.GLOAS), any())).thenReturn(GLOAS_DIGEST);
    when(recentChainData.getForkDigest(eq(SpecMilestone.HEZE), any())).thenReturn(HEZE_DIGEST);
    when(recentChainData.getMilestoneByForkDigest(GLOAS_DIGEST))
        .thenReturn(Optional.of(SpecMilestone.GLOAS));
    when(recentChainData.getMilestoneByForkDigest(HEZE_DIGEST))
        .thenReturn(Optional.of(SpecMilestone.HEZE));
  }

  @Test
  void a4LastGloasEnvelopeIsServedInTheGloasSchema() {
    assertServedInOwnSchema(spec.computeStartSlotAtEpoch(UInt64.ONE).minus(1), GLOAS_DIGEST);
  }

  @Test
  void a4FirstHezeEnvelopeIsServedInTheHezeSchema() {
    assertServedInOwnSchema(spec.computeStartSlotAtEpoch(UInt64.ONE), HEZE_DIGEST);
  }

  private void assertServedInOwnSchema(final UInt64 slot, final Bytes4 expectedDigest) {
    final SignedExecutionPayloadEnvelope envelope =
        dataStructureUtil.randomSignedExecutionPayloadEnvelope(slot.longValue());

    assertThat(codec.encodeContext(envelope)).isEqualTo(expectedDigest.getWrappedBytes());
    final SignedExecutionPayloadEnvelope decoded =
        codec
            .getSchemaFromContext(expectedDigest)
            .orElseThrow()
            .sszDeserialize(envelope.sszSerialize());
    assertThat(decoded).isEqualTo(envelope);
    assertThat(decoded.getMessage().getSchema())
        .isSameAs(
            SchemaDefinitionsGloas.required(spec.atSlot(slot).getSchemaDefinitions())
                .getExecutionPayloadEnvelopeSchema());
  }
}
