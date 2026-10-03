/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

public class ChannelRequestBudgetTest {
    @Test public void admitsEightChannelsInBothDirectionsPlusControl() {
        ChannelRequestBudget budget = new ChannelRequestBudget();
        for (int index = 0; index < 8 * 3; index++) budget.acquire(ChannelRequestBudget.MAX_ENCODED_SIZE);
        for (int index = 0; index < 8 * 3; index++) budget.release();
    }

    @Test public void rejectsOverflowAndRestoresCapacity() {
        ChannelRequestBudget budget = new ChannelRequestBudget();
        for (int index = 0; index < ChannelRequestBudget.MAX_PENDING; index++) budget.acquire(1);
        assertThrows(IllegalArgumentException.class, () -> budget.acquire(1));
        budget.release();
        budget.acquire(1);
        for (int index = 0; index < ChannelRequestBudget.MAX_PENDING; index++) budget.release();
        for (int index = 0; index < ChannelRequestBudget.MAX_PENDING; index++) budget.acquire(1);
    }

    @Test public void rejectsOversizedEnvelopeWithoutConsumingCapacity() {
        ChannelRequestBudget budget = new ChannelRequestBudget();
        assertThrows(IllegalArgumentException.class, () -> budget.acquire(ChannelRequestBudget.MAX_ENCODED_SIZE + 1));
        for (int index = 0; index < ChannelRequestBudget.MAX_PENDING; index++) budget.acquire(1);
    }
}
