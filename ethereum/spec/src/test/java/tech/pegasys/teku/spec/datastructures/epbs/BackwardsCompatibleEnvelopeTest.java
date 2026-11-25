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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszUInt64;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelopeSchema;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadSchema;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;

class BackwardsCompatibleEnvelopeTest {

  private static final List<String> KEPT_FIELDS =
      List.of("payload", "execution_requests", "beacon_block_root", "parent_beacon_block_root");
  private static final List<Long> KEPT_GINDICES = List.of(4L, 40L, 42L, 43L);

  private final SchemaDefinitionsGloas gloas =
      SchemaDefinitionsGloas.required(
          TestSpecFactory.createMinimalGloas().getGenesisSchemaDefinitions());
  private final SchemaDefinitionsGloas heze =
      SchemaDefinitionsGloas.required(
          TestSpecFactory.createMinimalHeze().getGenesisSchemaDefinitions());

  @Test
  void ae1HezeEnvelopeGapsBuilderIndexAndKeepsEveryOtherFieldInPlace() {
    assertThat(gindices(gloas.getExecutionPayloadEnvelopeSchema())).isEqualTo(KEPT_GINDICES);
    assertThat(gindices(heze.getExecutionPayloadEnvelopeSchema())).isEqualTo(KEPT_GINDICES);
    assertThat(heze.getExecutionPayloadEnvelopeSchema().getFieldNames())
        .doesNotContain("builder_index");
    assertThat(heze.getBlindedExecutionPayloadEnvelopeSchema().getFieldNames())
        .doesNotContain("builder_index");
  }

  @Test
  void ae2EnvelopeRootsMatchSpec() {
    assertThat(envelope(gloas).hashTreeRoot())
        .isEqualTo(
            Bytes32.fromHexString(
                "0x8046703d64f2dafda2e163c67506c5aee2088da2f2aaa0d530fa8a335232a95d"));
    assertThat(envelope(heze).hashTreeRoot())
        .isEqualTo(
            Bytes32.fromHexString(
                "0xd35e8e62fc0db4b16c387f9ae07ff7bdd4ee3021d5511312ab035db77e7d20cc"));
  }

  @Test
  void ae3HezeEnvelopeBytesAreShorterByBuilderIndex() {
    assertThat(envelope(gloas).sszSerialize().size()).isEqualTo(640);
    assertThat(envelope(heze).sszSerialize().size()).isEqualTo(632);
  }

  @Test
  void ae4HezeEnvelopeBlindsToTheSameRoot() {
    final ExecutionPayloadEnvelope envelope = envelope(heze);
    assertThat(envelope.blind(heze).hashTreeRoot()).isEqualTo(envelope.hashTreeRoot());
  }

  private static List<Long> gindices(final ExecutionPayloadEnvelopeSchema schema) {
    return KEPT_FIELDS.stream()
        .map(name -> schema.getChildGeneralizedIndex(schema.getFieldIndex(name)))
        .toList();
  }

  private static ExecutionPayloadEnvelope envelope(final SchemaDefinitionsGloas schemas) {
    return schemas
        .getExecutionPayloadEnvelopeSchema()
        .create(
            payloadAtSlot(schemas.getExecutionPayloadSchema(), 9),
            schemas.getExecutionRequestsSchema().getDefault(),
            UInt64.valueOf(7),
            Bytes32.wrap(Bytes.repeat((byte) 0x22, 32)),
            Bytes32.wrap(Bytes.repeat((byte) 0x33, 32)));
  }

  private static ExecutionPayload payloadAtSlot(
      final ExecutionPayloadSchema<?> schema, final long slot) {
    final ExecutionPayload empty = schema.getDefault();
    final List<SszData> fields =
        new ArrayList<>(IntStream.range(0, empty.size()).mapToObj(empty::get).toList());
    fields.set(schema.getFieldIndex("slot_number"), SszUInt64.of(UInt64.valueOf(slot)));
    return schema.createFromFieldValues(fields);
  }
}
