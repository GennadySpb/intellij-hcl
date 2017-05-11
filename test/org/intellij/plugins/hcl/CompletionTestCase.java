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
package org.intellij.plugins.hcl;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.BooleanFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public abstract class CompletionTestCase extends LightPlatformCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(CompletionTestCase.class);
  protected int myCompleteInvocationCount = 1;

  protected abstract String getFileName();

  protected abstract Language getExpectedLanguage();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myCompleteInvocationCount = 1;
  }

  protected void doBasicCompletionTest(String text, Collection<String> expected) {
    doBasicCompletionTest(text, expected.size(), expected.toArray(new String[expected.size()]));
  }

  protected void doBasicCompletionTest(String text, final int expectedAllSize, final String... expected) {
    doBasicCompletionTest(text, getPartialMatcher(expectedAllSize, expected));
  }
  protected void doBasicCompletionTest(String text, final String... expected) {
    doBasicCompletionTest(text, getPartialMatcher(expected));
  }

  protected void doBasicCompletionTest(String text, BooleanFunction<Collection<String>> matcher) {
    final PsiFile psiFile = myFixture.configureByText(getFileName(), text);
    System.out.println("PsiFile = " + DebugUtil.psiToString(psiFile, true));
    assertEquals(getExpectedLanguage(), psiFile.getLanguage());
    final LookupElement[] elements = myFixture.complete(CompletionType.BASIC, myCompleteInvocationCount);
    assertNotNull(elements);
    final List<String> strings = myFixture.getLookupElementStrings();
    assertNotNull(strings);
    System.out.println("LookupStrings = " + strings);
    assertTrue("Matcher expected to return true", matcher.fun(strings));
  }

  public static abstract class Matcher implements BooleanFunction<Collection<String>> {
    public static Matcher and(final Matcher first, final Matcher second, final Matcher... rest){
      return new Matcher() {
        @Override
        public boolean fun(Collection<String> strings) {
          if (!first.fun(strings)) return false;
          if (!second.fun(strings)) return false;
          for (Matcher matcher : rest) {
            if (!matcher.fun(strings)) return false;
          }
          return true;
        }
      };
    }

    public static Matcher all(final String... unexpected) {
      return new Matcher() {
        @Override
        public boolean fun(Collection<String> strings) {
          then(strings).contains(unexpected);
          return true;
        }
      };
    }
    public static Matcher not(final String... unexpected) {
      return new Matcher() {
        @Override
        public boolean fun(Collection<String> strings) {
          then(strings).doesNotContain(unexpected);
          return true;
        }
      };
    }
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getExactMatcher(final String... expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).containsOnly(expectedPart);
        return true;
      }
    };
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final String... expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains(expectedPart);
        return true;
      }
    };
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final Collection<String> expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).containsAll(expectedPart);
        return true;
      }
    };
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final int expectedAllSize, final String... expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings)
            .contains(expectedPart)
            .hasSize(expectedAllSize);
        return true;
      }
    };
  }
}
