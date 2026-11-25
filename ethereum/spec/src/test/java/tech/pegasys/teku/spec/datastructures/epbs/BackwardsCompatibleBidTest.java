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

package tech.pegasys.teku.spec.datastructures.epbs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.LongStream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadBid;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadBidSchema;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;

class BackwardsCompatibleBidTest {

  private static final List<Long> GLOAS_FIELD_GINDICES =
      List.of(4L, 40L, 41L, 42L, 43L, 352L, 353L, 354L, 355L, 356L, 357L, 358L);
  private static final long PAYLOAD_ROOT_GINDEX = 360L;
  private static final Bytes32 PAYLOAD_ROOT = fill(0x77);

  private final ExecutionPayloadBidSchema gloas = bidSchema(TestSpecFactory.createMinimalGloas());
  private final ExecutionPayloadBidSchema heze = bidSchema(TestSpecFactory.createMinimalHeze());

  @Test
  void bc1GloasBidRootMatchesSpec() {
    assertThat(bid(gloas).hashTreeRoot())
        .isEqualTo(
            Bytes32.fromHexString(
                "0x0b6254768a93a05e34861c24a1d78fd4e5fb119ed08d95598c25861971af3f39"));
  }

  @Test
  void bc2HezeBidKeepsEveryGloasFieldInPlace() {
    assertThat(gindices(gloas)).isEqualTo(GLOAS_FIELD_GINDICES);
    assertThat(gindices(heze)).isEqualTo(GLOAS_FIELD_GINDICES);
    assertThat(heze.getChildGeneralizedIndex(heze.getFieldIndex("payload_root")))
        .isEqualTo(PAYLOAD_ROOT_GINDEX);
  }

  @Test
  void bc3HezeBidRootMatchesSpec() {
    assertThat(bid(heze).withPayloadRoot(PAYLOAD_ROOT).hashTreeRoot())
        .isEqualTo(
            Bytes32.fromHexString(
                "0xb63dbe69c8573f2aa90843ba9a25cb4a116217b42967b461c326b3bbcb5c381f"));
  }

  @Test
  void bc4HezeBidCarriesPayloadRootAndGloasDoesNot() {
    assertThat(bid(heze).withPayloadRoot(PAYLOAD_ROOT).getPayloadRoot()).contains(PAYLOAD_ROOT);
    assertThat(bid(gloas).getPayloadRoot()).isEmpty();
  }

  private static List<Long> gindices(final ExecutionPayloadBidSchema schema) {
    return LongStream.range(0, 12).map(schema::getChildGeneralizedIndex).boxed().toList();
  }

  private static ExecutionPayloadBid bid(final ExecutionPayloadBidSchema schema) {
    return schema.create(
        fill(0x11),
        fill(0x22),
        fill(0x33),
        fill(0x44),
        new Bytes20(Bytes.repeat((byte) 0x55, 20)),
        UInt64.valueOf(30_000_000),
        UInt64.valueOf(7),
        UInt64.valueOf(9),
        UInt64.valueOf(1000),
        UInt64.ZERO,
        schema.getBlobKzgCommitmentsSchema().of(),
        fill(0x66));
  }

  private static ExecutionPayloadBidSchema bidSchema(final Spec spec) {
    return SchemaDefinitionsGloas.required(spec.getGenesisSchemaDefinitions())
        .getExecutionPayloadBidSchema();
  }

  private static Bytes32 fill(final int value) {
    return Bytes32.wrap(Bytes.repeat((byte) value, 32));
  }
}
