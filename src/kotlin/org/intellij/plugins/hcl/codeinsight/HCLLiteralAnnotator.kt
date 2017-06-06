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
package org.intellij.plugins.hcl.codeinsight

import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.intellij.plugins.hcl.HCLSyntaxHighlighterFactory
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.inspection.TFVARSIncorrectElementInspection
import java.util.regex.Pattern

/**
 * Inspired by com.intellij.json.codeinsight.JsonLiteralAnnotator
 */
class HCLLiteralAnnotator : Annotator {
  companion object {
    // TODO: Check HCL supported escapes
    @Language("RegExp")
    private val COMMON_REGEX = "[\\\\abfntrv]|([0-7]{3})|(X[0-9a-fA-F]{2})|(u[0-9a-fA-F]{4})|(U[0-9a-fA-F]{8})"
    private val DQS_VALID_ESCAPE = Pattern.compile("\\\\(\"|$COMMON_REGEX)")
    private val SQS_VALID_ESCAPE = Pattern.compile("\\\\(\'|$COMMON_REGEX)")
    // TODO: AFAIK that should be handled by lexer/parser
    private val VALID_NUMBER_LITERAL = Pattern.compile("-?(0x)?([0-9])\\d*(\\.\\d+)?([e][+-]?\\d+)?([kmgb][b]?)?", Pattern.CASE_INSENSITIVE)
    private val VALID_HEX_NUMBER_LITERAL = Pattern.compile("-?(0x)?([0-9a-f])+", Pattern.CASE_INSENSITIVE)

    private val DEBUG = ApplicationManager.getApplication().isUnitTestMode
    private fun addBlockNameAnnotation(element: PsiElement, holder: AnnotationHolder, name: String) = holder.createInfoAnnotation(element, if (DEBUG) name else null)
  }

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val text = HCLPsiUtil.getElementTextWithoutHostEscaping(element)
    if (element is HCLStringLiteral || element is HCLIdentifier) {
      val parent = element.parent
      if (HCLPsiUtil.isPropertyKey(element)) {
        holder.createInfoAnnotation(element, if (DEBUG) "property key" else null).textAttributes = HCLSyntaxHighlighterFactory.HCL_PROPERTY_KEY
      } else if (parent is HCLBlock) {
        val ne = parent.nameElements
        if (ne.size == 1 && ne[0] === element) {
          holder.createInfoAnnotation(element, if (DEBUG) "block only name identifier" else null).textAttributes = HCLSyntaxHighlighterFactory.HCL_BLOCK_ONLY_NAME_KEY
        } else for (i in ne.indices) {
          if (ne[i] === element) {
            var annotation: Annotation?
            if (i == ne.lastIndex) {
              annotation = addBlockNameAnnotation(element, holder, "block name identifier")
              annotation.textAttributes = HCLSyntaxHighlighterFactory.HCL_BLOCK_NAME_KEY
            } else if (i == 0) {
              annotation = addBlockNameAnnotation(element, holder, "block type 1 element")
              annotation.textAttributes = HCLSyntaxHighlighterFactory.HCL_BLOCK_FIRST_TYPE_KEY
              annotation = null
            } else if (i == 1) {
              annotation = addBlockNameAnnotation(element, holder, "block type 2 element")
              annotation.textAttributes = HCLSyntaxHighlighterFactory.HCL_BLOCK_SECOND_TYPE_KEY
            } else {
              annotation = addBlockNameAnnotation(element, holder, "block type 3+ element")
              annotation.textAttributes = HCLSyntaxHighlighterFactory.HCL_BLOCK_OTHER_TYPES_KEY
            }
            if (annotation != null && element is HCLIdentifier) {
              annotation.registerUniversalFix(TFVARSIncorrectElementInspection.ConvertToHCLStringQuickFix(element), null, null)
            }
            break
          }
        }
      }
    }
    if (element is HCLStringLiteral) {
      val length = text.length

      // Check that string literal is closed properly
      if (length <= 1 || text[0] != text[length - 1] || HCLPsiUtil.isEscapedChar(text, length - 1)) {
        holder.createErrorAnnotation(element, "Missing closing quote").registerUniversalFix(AddClosingQuoteQuickFix(element), null, null)
      }

      val pattern = when (element.quoteSymbol) {
        '\'' -> SQS_VALID_ESCAPE
        '\"' -> DQS_VALID_ESCAPE
        else -> throw IllegalStateException("Unexpected string quote symbol '${element.quoteSymbol}'")
      }

      val elementOffset = element.getTextOffset()
      for (fragment in element.textFragments) {
        val fragmentText = fragment.getSecond()
        if (fragmentText.length > 1 && fragmentText[0] == '\\' && !pattern.matcher(fragmentText).matches()) {
          val shifted = fragment.getFirst().shiftRight(elementOffset)
          val c = fragmentText[1]
          val errText = when (c) {
            in '0'..'7' -> "Illegal octal escape sequence"
            'X' -> "Illegal hex escape sequence"
            'u', 'U' -> "Illegal unicode escape sequence"
            else -> "Illegal escape sequence"
          }
          errText.let { holder.createErrorAnnotation(shifted, errText) }
        }
      }
    } else if (element is HCLNumberLiteral) {
      if (!VALID_NUMBER_LITERAL.matcher(text).matches() && !VALID_HEX_NUMBER_LITERAL.matcher(text).matches()) {
        holder.createErrorAnnotation(element, "Illegal number literal")
      }
    }
  }
}

