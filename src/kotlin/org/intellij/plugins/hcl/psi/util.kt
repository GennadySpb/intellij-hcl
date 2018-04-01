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
package org.intellij.plugins.hcl.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

fun HCLBlock.getNameElementUnquoted(i: Int): String? {
  val elements = this.nameElements
  if (elements.size < i + 1) return null
  val element = elements[i]
  @Suppress("USELESS_CAST")
  return when (element) {
    is PsiNamedElement -> (element as PsiNamedElement).name
    is HCLIdentifier -> element.id
    is HCLStringLiteral -> element.value
    else -> null
  }
}

fun PsiElement.getPrevSiblingNonWhiteSpace(): PsiElement? {
  var prev = this.prevSibling
  while (prev != null && prev is PsiWhiteSpace) {
    prev = prev.prevSibling
  }
  return prev
}

fun PsiElement.getNextSiblingNonWhiteSpace(): PsiElement? {
  var prev = this.nextSibling
  while (prev != null && prev is PsiWhiteSpace) {
    prev = prev.nextSibling
  }
  return prev
}

fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.afterSiblingSkipping2(skip: ElementPattern<out Any>, pattern: ElementPattern<out PsiElement>): Self {
  return with(object : PatternCondition<T>("afterSiblingSkipping2") {
    override fun accepts(t: T, context: ProcessingContext): Boolean {
      var o = t.prevSibling
      while (o != null) {
        if (!skip.accepts(o, context)) {
          return pattern.accepts(o, context)
        }
        o = o.prevSibling
      }
      return false
    }
  })
}

fun PsiElement.isInHCLFileWithInterpolations(): Boolean {
  var file = containingFile
  if (file == null) {
    Logger.getInstance("#org.intellij.plugins.hcl.psi.UtilKt").warn("Cannot obtain 'containingFile' for element $this")
    return false
  }
  if (file.containingDirectory == null) {
    // Probably injected language
    file = InjectedLanguageManager.getInstance(project).getTopLevelFile(this)
  }
  return file is HCLFile && file.isInterpolationsAllowed()
}

fun <T : PsiElement> PsiElement?.getParent(aClass: Class<T>, strict: Boolean = true): T? {
  if (this == null) return null
  return PsiTreeUtil.getParentOfType(this, aClass, strict)
}