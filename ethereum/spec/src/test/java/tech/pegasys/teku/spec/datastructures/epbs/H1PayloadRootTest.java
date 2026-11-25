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
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.dishonestPayloadRoot;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.honestPayloadRoot;
import static tech.pegasys.teku.spec.datastructures.epbs.PairingPrimitives.tamperPayload;

import org.junit.jupiter.api.Test;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.epbs.versions.gloas.ExecutionPayloadEnvelope;
import tech.pegasys.teku.spec.util.DataStructureUtil;

class H1PayloadRootTest {

  private final Spec spec = TestSpecFactory.createMinimalGloas();
  private final ExecutionPayloadEnvelope envelope =
      new DataStructureUtil(spec).randomExecutionPayloadEnvelope();

  @Test
  void h1AcceptsHonestPayloadRoot() {
    assertThat(envelope.isPayloadRootValid(honestPayloadRoot(envelope))).isTrue();
  }

  @Test
  void h1RejectsDishonestPayloadRoot() {
    assertThat(envelope.isPayloadRootValid(dishonestPayloadRoot(envelope))).isFalse();
  }

  @Test
  void h1RejectsTamperedPayloadUnderHonestRoot() {
    assertThat(tamperPayload(envelope).isPayloadRootValid(honestPayloadRoot(envelope))).isFalse();
  }
}
