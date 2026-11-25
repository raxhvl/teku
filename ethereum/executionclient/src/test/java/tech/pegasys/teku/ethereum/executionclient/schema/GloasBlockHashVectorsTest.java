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

package tech.pegasys.teku.ethereum.executionclient.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadBlockHashCalculator;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionRequests;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionRequestsDataCodec;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;

class GloasBlockHashVectorsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Spec SPEC = TestSpecFactory.createMinimalGloas();
  private static final SchemaDefinitionsGloas SCHEMAS =
      SchemaDefinitionsGloas.required(SPEC.getGenesisSchemaDefinitions());
  private static final ExecutionRequestsDataCodec CODEC =
      SPEC.getExecutionRequestsDataCodec(UInt64.ZERO);

  @ParameterizedTest(name = "{0}")
  @MethodSource("vectors")
  void g1BlockHashMatchesEl(final String name, final JsonNode vector) throws IOException {
    final ObjectNode payloadJson = vector.get("executionPayload").deepCopy();
    final Bytes32 expected = Bytes32.fromHexString(payloadJson.get("blockHash").asText());
    payloadJson.put("blockHash", Bytes32.ZERO.toHexString());

    final ExecutionPayload payload =
        MAPPER
            .treeToValue(payloadJson, ExecutionPayloadV4.class)
            .asInternalExecutionPayload(SCHEMAS.getExecutionPayloadSchema());
    final List<Bytes> requestsData = bytesList(vector.get("executionRequests"));
    final ExecutionRequests requests = CODEC.decode(requestsData);
    final ExecutionPayloadEnvelope envelope =
        SCHEMAS
            .getExecutionPayloadEnvelopeSchema()
            .create(
                payload,
                requests,
                UInt64.ZERO,
                Bytes32.ZERO,
                Bytes32.fromHexString(vector.get("parentBeaconBlockRoot").asText()));

    assertThat(CODEC.encode(requests)).isEqualTo(requestsData);

    assertThat(ExecutionPayloadBlockHashCalculator.computeGloasBlockHash(envelope, CODEC))
        .isEqualTo(expected);
  }

  static Stream<Arguments> vectors() throws IOException {
    try (InputStream in =
        GloasBlockHashVectorsTest.class.getResourceAsStream("gloas_block_hash_vectors.json")) {
      final JsonNode vectors = MAPPER.readTree(in).get("vectors");
      return StreamSupport.stream(vectors.spliterator(), false)
          .map(v -> Arguments.of(v.get("name").asText(), v))
          .toList()
          .stream();
    }
  }

  private static List<Bytes> bytesList(final JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(n -> Bytes.fromHexString(n.asText()))
        .toList();
  }
}
