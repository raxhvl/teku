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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.ssz.SszContainer;
import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszUInt64;
import tech.pegasys.teku.infrastructure.ssz.schema.SszContainerSchema;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.infrastructure.ssz.schema.impl.AbstractSszContainerSchema.NamedSchema;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

/** Gloas envelopes have 5 fields. Heze gaps builder_index, so the other 4 keep their place. */
final class EnvelopeFields {

  static final String BUILDER_INDEX = "builder_index";
  static final String BEACON_BLOCK_ROOT = "beacon_block_root";
  static final String PARENT_BEACON_BLOCK_ROOT = "parent_beacon_block_root";

  private EnvelopeFields() {}

  static boolean[] activeFields(final boolean withoutBuilderIndex) {
    return new boolean[] {true, true, !withoutBuilderIndex, true, true};
  }

  static List<NamedSchema<?>> fields(
      final NamedSchema<?> payload,
      final NamedSchema<?> executionRequests,
      final boolean withoutBuilderIndex) {
    final List<NamedSchema<?>> fields = new ArrayList<>(List.of(payload, executionRequests));
    if (!withoutBuilderIndex) {
      fields.add(NamedSchema.of(BUILDER_INDEX, SszPrimitiveSchemas.UINT64_SCHEMA));
    }
    fields.add(NamedSchema.of(BEACON_BLOCK_ROOT, SszPrimitiveSchemas.BYTES32_SCHEMA));
    fields.add(NamedSchema.of(PARENT_BEACON_BLOCK_ROOT, SszPrimitiveSchemas.BYTES32_SCHEMA));
    return fields;
  }

  static SszData[] values(
      final SszContainerSchema<?> schema,
      final SszData payload,
      final SszData executionRequests,
      final UInt64 builderIndex,
      final Bytes32 beaconBlockRoot,
      final Bytes32 parentBeaconBlockRoot) {
    final List<SszData> values = new ArrayList<>(List.of(payload, executionRequests));
    if (schema.getFieldNames().contains(BUILDER_INDEX)) {
      values.add(SszUInt64.of(builderIndex));
    }
    values.add(SszBytes32.of(beaconBlockRoot));
    values.add(SszBytes32.of(parentBeaconBlockRoot));
    return values.toArray(SszData[]::new);
  }

  static Optional<UInt64> builderIndex(final SszContainer container) {
    return container.getSchema().getFieldNames().contains(BUILDER_INDEX)
        ? Optional.of(
            container.<SszUInt64>getAny(container.getSchema().getFieldIndex(BUILDER_INDEX)).get())
        : Optional.empty();
  }

  static Bytes32 bytes32(final SszContainer container, final String name) {
    return container.<SszBytes32>getAny(container.getSchema().getFieldIndex(name)).get();
  }
}
