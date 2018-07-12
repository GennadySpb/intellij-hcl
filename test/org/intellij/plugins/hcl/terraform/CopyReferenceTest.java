/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform;

import com.intellij.ide.actions.CopyReferenceAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import org.intellij.plugins.hcl.psi.*;
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator;

import java.util.List;

public class CopyReferenceTest extends LightPlatformTestCase {
  protected HCLElementGenerator myElementGenerator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myElementGenerator = new TerraformElementGenerator(getProject());
  }

  public void testResourceDefinitionBlock() throws Exception {
    HCLBlock block = (HCLBlock) myElementGenerator.createDummyFile("resource 'A' 'B' {}").getFirstChild();
    checkFQN("A.B", block.getNameElements()[2]);
    checkFQN("A.B", block.getNameElements()[1]);
    checkFQN("A.B", block.getNameElements()[0]);
    checkFQN("A.B", block);
  }

  public void testResourceUsageInDependsOn() throws Exception {
    PsiFile file = myElementGenerator.createDummyFile("resource x y {}\nresource a b {depends_on=['x.y']}");
    HCLBlock[] blocks = PsiTreeUtil.getChildrenOfType(file, HCLBlock.class);
    assertNotNull(blocks);
    HCLObject object = blocks[1].getObject();
    assertNotNull(object);
    HCLProperty property = object.findProperty("depends_on");
    assertNotNull(property);
    List<HCLExpression> valueList = ((HCLArray) (property.getValue())).getExpressionList();
    assertNotNull(valueList);
    HCLExpression value = valueList.get(0);
    assertNotNull(value);
    checkFQN("x.y", value);
  }

  public void testDataSourceUsageInDependsOn() throws Exception {
    PsiFile file = myElementGenerator.createDummyFile("data x y {}\nresource a b {depends_on=['data.x.y']}");
    HCLBlock[] blocks = PsiTreeUtil.getChildrenOfType(file, HCLBlock.class);
    assertNotNull(blocks);
    HCLObject object = blocks[1].getObject();
    assertNotNull(object);
    HCLProperty property = object.findProperty("depends_on");
    assertNotNull(property);
    List<HCLExpression> valueList = ((HCLArray) (property.getValue())).getExpressionList();
    assertNotNull(valueList);
    HCLExpression value = valueList.get(0);
    assertNotNull(value);
    checkFQN("data.x.y", value);
  }

  private void checkFQN(String expected, PsiElement element) {
    String fqn = CopyReferenceAction.elementToFqn(element);
    // Emulate logic from CopyReferenceAction which uses Editor internally
    if (fqn == null) {
      PsiReference[] references = element.getReferences();
      for (PsiReference reference : references) {
        PsiElement resolve = reference.resolve();
        if (resolve != null) {
          fqn = CopyReferenceAction.elementToFqn(resolve);
          if (fqn != null) break;
        }
      }
    }
    assertEquals(expected, fqn);
  }
}
