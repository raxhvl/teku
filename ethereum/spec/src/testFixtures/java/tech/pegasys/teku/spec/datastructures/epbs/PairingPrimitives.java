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

import java.util.ArrayList;
import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.schema.SszContainerSchema;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadBlockHashCalculator;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionRequestsDataCodec;

public final class PairingPrimitives {

  private PairingPrimitives() {}

  public static ExecutionPayloadEnvelope honestGloas(
      final ExecutionPayloadEnvelope envelope, final ExecutionRequestsDataCodec codec) {
    return withPayload(
        envelope,
        withField(
            envelope.getPayload(),
            "block_hash",
            ExecutionPayloadBlockHashCalculator.computeGloasBlockHash(envelope, codec)));
  }

  public static ExecutionPayloadEnvelope tamperPayload(final ExecutionPayloadEnvelope envelope) {
    final ExecutionPayload payload = envelope.getPayload();
    return withPayload(envelope, withField(payload, "state_root", payload.getStateRoot().not()));
  }

  public static Bytes32 honestPayloadRoot(final ExecutionPayloadEnvelope envelope) {
    return envelope.getPayload().hashTreeRoot();
  }

  public static Bytes32 dishonestPayloadRoot(final ExecutionPayloadEnvelope envelope) {
    return honestPayloadRoot(envelope).not();
  }

  public static ExecutionPayloadEnvelope withPayload(
      final ExecutionPayloadEnvelope envelope, final ExecutionPayload payload) {
    return envelope
        .getSchema()
        .create(
            payload,
            envelope.getExecutionRequests(),
            envelope.getOptionalBuilderIndex().orElse(UInt64.ZERO),
            envelope.getBeaconBlockRoot(),
            envelope.getParentBeaconBlockRoot());
  }

  private static ExecutionPayload withField(
      final ExecutionPayload source, final String fieldName, final Bytes32 value) {
    final SszContainerSchema<?> schema = source.getSchema();
    final List<SszData> fields = new ArrayList<>();
    for (int i = 0; i < schema.getFieldsCount(); i++) {
      fields.add(source.get(i));
    }
    fields.set(schema.getFieldIndex(fieldName), SszBytes32.of(value));
    return (ExecutionPayload) schema.createFromFieldValues(fields);
  }
}
