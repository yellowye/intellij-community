/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.command.impl;

import com.intellij.history.core.LocalHistoryFacade;
import com.intellij.history.core.changes.Change;
import com.intellij.history.integration.IdeaGateway;
import com.intellij.history.integration.revertion.ChangeRevertingVisitor;
import com.intellij.openapi.util.Ref;

import java.io.IOException;

public class ChangeRange {
  private final IdeaGateway myGateway;
  private final LocalHistoryFacade myVcs;
  private final long myFromChangeId;
  private final long myToChangeId;

  public ChangeRange(IdeaGateway gw, LocalHistoryFacade vcs, long change) {
    this(gw, vcs, change, change);
  }

  public ChangeRange(IdeaGateway gw, LocalHistoryFacade vcs, long fromChangeId, long toChangeId) {
    myGateway = gw;
    myVcs = vcs;
    myFromChangeId = fromChangeId;
    myToChangeId = toChangeId;
  }

  public ChangeRange revert(ChangeRange reverse) throws IOException {
    final Ref<Long> first = new Ref<Long>();
    final Ref<Long> last = new Ref<Long>();
    LocalHistoryFacade.Listener l = new LocalHistoryFacade.Listener() {
      public void changeAdded(Change c) {
        if (first.isNull()) first.set(c.getId());
        last.set(c.getId());
      }
    };
    myVcs.addListener(l, null);
    try {
      myVcs.accept(new ChangeRevertingVisitor(myGateway, myToChangeId, myFromChangeId));
    }
    catch(ChangeRevertingVisitor.RuntimeIOException e) {
      throw (IOException)e.getCause();
    }
    finally {
      myVcs.removeListener(l);
    }
    
    if (reverse != null) {
      if (first.isNull()) first.set(reverse.myFromChangeId);
      if (last.isNull()) last.set(reverse.myToChangeId);
    }
    return new ChangeRange(myGateway, myVcs, first.get(), last.get());
  }
}
