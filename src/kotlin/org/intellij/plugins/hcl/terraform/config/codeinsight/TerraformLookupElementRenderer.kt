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
package org.intellij.plugins.hcl.terraform.config.codeinsight

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.openapi.util.text.StringUtil
import org.intellij.plugins.hcl.Icons
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import javax.swing.Icon

class TerraformLookupElementRenderer : LookupElementRenderer<LookupElement>() {
  override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
    presentation.itemText = element.lookupString
    val obj = element.`object`
    if (obj is PropertyOrBlockType) {
      if (obj.property != null) {
        presentation.icon = Icons.Property
        presentation.isItemTextBold = obj.property.required
        presentation.isStrikeout = obj.property.deprecated != null
        presentation.setTailText(trimDescription(obj.property.description), true)
        presentation.setTypeText(obj.property.type.name, getTypeIcon(obj.property.type))
      } else if (obj.block != null) {
        presentation.icon = Icons.Object
        presentation.isItemTextBold = obj.block.required
        presentation.isStrikeout = obj.block.deprecated != null
        presentation.setTailText(trimDescription(obj.block.description), true)
      }
    }
  }

  private fun trimDescription(description: String?): String? {
    if (description == null) return null
    return " " + StringUtil.shortenTextWithEllipsis(description, 55, 0, true)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun getTypeIcon(type: Type): Icon? {
    // TODO: Implement
    when (type) {
      Types.Identifier -> com.intellij.icons.AllIcons.Ide.LookupAlphanumeric
      Types.String -> com.intellij.icons.AllIcons.Ide.LookupAlphanumeric
      Types.Number -> com.intellij.icons.AllIcons.Ide.LookupRelevance
      Types.Array -> Icons.Array
      Types.Object -> Icons.Object
//      Types.StringWithInjection ->
//      Types.Boolean ->
//      Types.Null ->
//      Types.Invalid ->
//      Types.Any ->
    }
    return null
  }
}
