// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.navigation.ItemPresentation;

public interface HCLProperty extends HCLElement, PsiNameIdentifierOwner {

  @NotNull
  String getName();

  @NotNull
  HCLValue getNameElement();

  @Nullable
  HCLValue getValue();

  @Nullable
  ItemPresentation getPresentation();

}
