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

package tech.pegasys.teku.spec.datastructures.epbs.versions.gloas;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.impl.AbstractSszImmutableContainer;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.blocks.SlotAndBlockRoot;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionRequests;
import tech.pegasys.teku.spec.datastructures.execution.versions.gloas.ExecutionPayloadHeaderGloas;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsGloas;

public class BlindedExecutionPayloadEnvelope extends AbstractSszImmutableContainer {

  BlindedExecutionPayloadEnvelope(
      final BlindedExecutionPayloadEnvelopeSchema schema, final SszData[] values) {
    super(schema, values);
  }

  BlindedExecutionPayloadEnvelope(
      final BlindedExecutionPayloadEnvelopeSchema type, final TreeNode backingNode) {
    super(type, backingNode);
  }

  public ExecutionPayloadHeader getPayloadHeader() {
    return getAny(0);
  }

  public ExecutionRequests getExecutionRequests() {
    return getAny(1);
  }

  public Optional<UInt64> getOptionalBuilderIndex() {
    return EnvelopeFields.builderIndex(this);
  }

  public UInt64 getBuilderIndex() {
    return getOptionalBuilderIndex()
        .orElseThrow(() -> new IllegalStateException("Heze envelopes have no builder_index"));
  }

  public Bytes32 getBeaconBlockRoot() {
    return EnvelopeFields.bytes32(this, EnvelopeFields.BEACON_BLOCK_ROOT);
  }

  public Bytes32 getParentBeaconBlockRoot() {
    return EnvelopeFields.bytes32(this, EnvelopeFields.PARENT_BEACON_BLOCK_ROOT);
  }

  public UInt64 getSlot() {
    return ExecutionPayloadHeaderGloas.required(getPayloadHeader()).getSlotNumber();
  }

  public SlotAndBlockRoot getSlotAndBlockRoot() {
    return new SlotAndBlockRoot(getSlot(), getBeaconBlockRoot());
  }

  @Override
  public BlindedExecutionPayloadEnvelopeSchema getSchema() {
    return (BlindedExecutionPayloadEnvelopeSchema) super.getSchema();
  }

  public ExecutionPayloadEnvelope unblind(
      final SchemaDefinitionsGloas schemaDefinitions, final ExecutionPayload payload) {
    checkState(
        payload.hashTreeRoot().equals(getPayloadHeader().hashTreeRoot()),
        "payloadHeader root in blinded execution payload envelope does not match provided executionPayload root");
    final ExecutionPayloadEnvelope executionPayloadEnvelope =
        schemaDefinitions
            .getExecutionPayloadEnvelopeSchema()
            .create(
                payload,
                getExecutionRequests(),
                getOptionalBuilderIndex().orElse(UInt64.ZERO),
                getBeaconBlockRoot(),
                getParentBeaconBlockRoot());
    checkState(
        executionPayloadEnvelope.hashTreeRoot().equals(hashTreeRoot()),
        "unblinded execution payload envelope root does not match original envelope root");
    return executionPayloadEnvelope;
  }
}
