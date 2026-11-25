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

import static tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadBidSchema.GLOAS_FIELD_COUNT;
import static tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadFields.PAYLOAD_ROOT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.ethereum.execution.types.Eth1Address;
import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.SszList;
import tech.pegasys.teku.infrastructure.ssz.collections.SszByteVector;
import tech.pegasys.teku.infrastructure.ssz.impl.AbstractSszImmutableContainer;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszUInt64;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.type.SszKZGCommitment;

public class ExecutionPayloadBid extends AbstractSszImmutableContainer {

  protected ExecutionPayloadBid(
      final ExecutionPayloadBidSchema schema,
      final Bytes32 parentBlockHash,
      final Bytes32 parentBlockRoot,
      final Bytes32 blockHash,
      final Bytes32 prevRandao,
      final Bytes20 feeRecipient,
      final UInt64 gasLimit,
      final UInt64 builderIndex,
      final UInt64 slot,
      final UInt64 value,
      final UInt64 executionPayment,
      final SszList<SszKZGCommitment> blobKzgCommitments,
      final Bytes32 executionRequestsRoot) {
    super(
        schema,
        withDefaults(
            schema,
            SszBytes32.of(parentBlockHash),
            SszBytes32.of(parentBlockRoot),
            SszBytes32.of(blockHash),
            SszBytes32.of(prevRandao),
            SszByteVector.fromBytes(feeRecipient.getWrappedBytes()),
            SszUInt64.of(gasLimit),
            SszUInt64.of(builderIndex),
            SszUInt64.of(slot),
            SszUInt64.of(value),
            SszUInt64.of(executionPayment),
            blobKzgCommitments,
            SszBytes32.of(executionRequestsRoot)));
  }

  protected ExecutionPayloadBid(
      final ExecutionPayloadBidSchema schema, final TreeNode backingTree) {
    super(schema, backingTree);
  }

  private ExecutionPayloadBid(final ExecutionPayloadBidSchema schema, final SszData[] fields) {
    super(schema, fields);
  }

  private static SszData[] withDefaults(
      final ExecutionPayloadBidSchema schema, final SszData... gloasFields) {
    final List<SszData> fields = new ArrayList<>(List.of(gloasFields));
    IntStream.range(GLOAS_FIELD_COUNT, schema.getFieldsCount())
        .forEach(i -> fields.add(schema.getChildSchema(i).getDefault()));
    return fields.toArray(SszData[]::new);
  }

  public Optional<Bytes32> getPayloadRoot() {
    return getSchema().getFieldNames().contains(PAYLOAD_ROOT.getSszFieldName())
        ? Optional.of(this.<SszBytes32>getAny(getSchema().getFieldIndex(PAYLOAD_ROOT)).get())
        : Optional.empty();
  }

  public ExecutionPayloadBid withPayloadRoot(final Bytes32 payloadRoot) {
    final SszData[] fields = IntStream.range(0, size()).mapToObj(this::get).toArray(SszData[]::new);
    fields[getSchema().getFieldIndex(PAYLOAD_ROOT)] = SszBytes32.of(payloadRoot);
    return new ExecutionPayloadBid(getSchema(), fields);
  }

  public Bytes32 getParentBlockHash() {
    return this.<SszBytes32>getAny(0).get();
  }

  public Bytes32 getParentBlockRoot() {
    return this.<SszBytes32>getAny(1).get();
  }

  public Bytes32 getBlockHash() {
    return this.<SszBytes32>getAny(2).get();
  }

  public Bytes32 getPrevRandao() {
    return this.<SszBytes32>getAny(3).get();
  }

  public Eth1Address getFeeRecipient() {
    return Eth1Address.fromBytes(this.<SszByteVector>getAny(4).getBytes());
  }

  public UInt64 getGasLimit() {
    return this.<SszUInt64>getAny(5).get();
  }

  public UInt64 getBuilderIndex() {
    return this.<SszUInt64>getAny(6).get();
  }

  public UInt64 getSlot() {
    return this.<SszUInt64>getAny(7).get();
  }

  public UInt64 getValue() {
    return this.<SszUInt64>getAny(8).get();
  }

  public UInt64 getExecutionPayment() {
    return this.<SszUInt64>getAny(9).get();
  }

  public SszList<SszKZGCommitment> getBlobKzgCommitments() {
    return getAny(10);
  }

  public Bytes32 getExecutionRequestsRoot() {
    return this.<SszBytes32>getAny(11).get();
  }

  @Override
  public ExecutionPayloadBidSchema getSchema() {
    return (ExecutionPayloadBidSchema) super.getSchema();
  }
}
