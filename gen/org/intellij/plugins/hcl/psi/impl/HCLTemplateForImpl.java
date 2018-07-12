// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.intellij.plugins.hcl.psi.*;

public class HCLTemplateForImpl extends ASTWrapperPsiElement implements HCLTemplateFor {

  public HCLTemplateForImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitTemplateFor(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HCLExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLExpression.class);
  }

  @Override
  @NotNull
  public HCLTemplate getTemplate() {
    return findNotNullChildByClass(HCLTemplate.class);
  }

}
