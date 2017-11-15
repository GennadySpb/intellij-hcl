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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.NullableFunction
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns

class TFDuplicatedOutputInspection : TFDuplicatedInspectionBase() {

  override fun createVisitor(holder: ProblemsHolder): PsiElementVisitor {
    return MyEV(holder)
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      val duplicates = getDuplicates(block) ?: return
      val name = block.getNameElementUnquoted(1) ?: return
      holder.registerProblem(block, "Output '$name' declared multiple times", ProblemHighlightType.GENERIC_ERROR, *getFixes(block, duplicates))
    }
  }

  private fun getDuplicates(block: HCLBlock): List<HCLBlock>? {
    if (!TerraformPatterns.OutputRootBlock.accepts(block)) return null
    val inOverride = TerraformPatterns.ConfigOverrideFile.accepts(block.containingFile)
    if (inOverride) return null

    val module = block.getTerraformModule()

    val name = block.getNameElementUnquoted(1) ?: return null

    val same = module.getDefinedOutputs().filter { name == it.getNameElementUnquoted(1) && !TerraformPatterns.ConfigOverrideFile.accepts(it.containingFile) }
    if (same.isEmpty()) return null
    if (same.size == 1) {
      assert(same.first() == block)
      return null
    }
    return same
  }

  private fun getFixes(block: HCLBlock, duplicates: List<HCLBlock>): Array<LocalQuickFix> {
    val fixes = ArrayList<LocalQuickFix>()

    val first = duplicates.first { it != block }
    first.containingFile?.virtualFile?.let { createNavigateToDupeFix(it, first.textOffset, duplicates.size <= 2)?.let { fixes.add(it) } }
    block.containingFile?.virtualFile?.let { createShowOtherDupesFix(it, block.textOffset, NullableFunction { param -> getDuplicates(param as HCLBlock) })?.let { fixes.add(it) } }

    fixes.add(DeleteOutputFix)
//    fixes.add(RenameOutputFix)
    return fixes.toTypedArray()
  }

  private object DeleteOutputFix : LocalQuickFixBase("Delete output"), LowPriorityAction {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val block = descriptor.psiElement as? HCLBlock ?: return
      ApplicationManager.getApplication().runWriteAction { block.delete() }
    }
  }

  private object RenameOutputFix : RenameQuickFix("Rename output") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val block = descriptor.psiElement as? HCLBlock ?: return
      invokeRenameRefactoring(project, block)
    }
  }
}

