/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib.intent;

import org.nlpcraft.mdllib.*;
import java.io.*;
import java.util.*;

/**
 * A context that is passed into {@link NCIntentSolver.IntentCallback callback} of the matched intent.
 *
 * @see NCIntentSolver.IntentCallback
 * @see NCIntentSolver#addIntent(NCIntentSolver.INTENT, NCIntentSolver.IntentCallback)
 */
public interface NCIntentSolverContext extends Serializable {
    /**
     * Gets ID of the matched intent.
     *
     * @return ID of the matched intent.
     */
    String getIntentId();

    /**
     * Query context from the original {@link NCModel#query(NCQueryContext)} method.
     *
     * @return Original query context.
     */
    NCQueryContext getQueryContext();

    /**
     * Gets a subset of tokens representing matched intent. This subset is grouped by the matched terms
     * where {@code null} sub-list defines an optional term. Order and index of sub-lists corresponds
     * to the order and index of terms in the matching intent. Note that unlike {@link #getVariant()} method
     * this method returns only subset of the tokens that were part of the matched intent. Specifically, it will
     * not return tokens for free words, stopwords or unmatched ("dangling") tokens.
     *
     * @return List of list of tokens representing matched intent.
     * @see #getVariant() 
     */
    List<List<NCToken>> getIntentTokens();

    /**
     * Gets sentence variant that produced the matching for this intent.
     * 
     * @return Sentence variant that produced the matching for this intent.
     * @see #getIntentTokens() 
     */
    NCVariant getVariant();

    /**
     * Indicates whether or not the intent match was exact.
     * <br><br>
     * An exact match means that for the intent to match it has to use all non-free word tokens
     * in the user input, i.e. only free word tokens can be left after the match. Non exact match
     * doesn't have this restriction. Note that non exact match should be used with a great care.
     * Non exact match completely ignores extra found user or system tokens (which are not part
     * of the intent template) which could have altered the matching outcome had they been included.
     * <br><br>
     * Intent callbacks can check this property and provide custom rejection message or send user input
     * to curation to ensure proper answer - if desired.
     *
     * @return {@code True} if the intent match was exact, {@code false} otherwise.
     */
    boolean isExactMatch();
}