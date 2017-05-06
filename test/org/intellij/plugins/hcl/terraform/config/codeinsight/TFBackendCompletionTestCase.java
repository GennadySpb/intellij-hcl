/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.hcl.terraform.config.codeinsight;

public class TFBackendCompletionTestCase extends TFBaseCompletionTestCase {
  public void testTerraformBlockAdvised() throws Exception {
    doBasicCompletionTest("<caret>", "terraform");
    doBasicCompletionTest("t<caret>", "terraform");
  }

  public void testBackendAllowedInTerraform() throws Exception {
    doBasicCompletionTest("terraform{<caret>}", "backend");
    doBasicCompletionTest("terraform{backend \"<caret>\" {}}", "s3");
    doBasicCompletionTest("terraform{backend <caret> {}}", "s3");

//    TODO: Investigate and uncomment. For now it's ok since autocomption handler on 'backend' would add name and braces
//    doBasicCompletionTest("terraform{backend \"<caret>\"}", "s3");
//    doBasicCompletionTest("terraform{backend <caret>}", "s3");
  }

  public void testPropertiesInBackend() throws Exception {
    doBasicCompletionTest("terraform{backend \"s3\" {<caret>}}", "bucket", "key");
  }
}
