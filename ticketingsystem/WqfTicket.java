package ticketingsystem;

import java.util.concurrent.atomic.AtomicBoolean;

public class WqfTicket {

    public static class RealTicket {
        long tid;
        int departure;
        int arrival;
        String passenger;
        LogicTicket root;
        RealTicket left, right;
        // use for push/pop
        RealTicket next;
        /// used for test if it is part of some realTicket, lazy deletion...
        AtomicBoolean isIndividualAndFree = new AtomicBoolean(true);

        public RealTicket(LogicTicket mRoot, RealTicket mLeft, RealTicket mRight, int mD, int mA)
        {
            tid = -1;   // means it is not being used.
            departure = mD;
            arrival = mA;
            root = mRoot;
            left = mLeft;
            right = mRight;
            next = null;
        }

        public boolean isOkToUse() { return isIndividualAndFree.compareAndSet(true, false); }
    }

    public static class LogicTicket {
        int coachId, seatId;
        // just use as a guard.
        RealTicket head = new RealTicket(this, null, null, -1, -1);

        public LogicTicket(int mCoachId, int mSeatId)
        {
            coachId = mCoachId;
            seatId = mSeatId;
        }

        public RealTicket initRealTicket(int stationNum)
        {
            RealTicket realTicket;
            realTicket = new WqfTicket.RealTicket(this, head, head, 1, stationNum);
            head.left = head.right = realTicket;
            return realTicket;
        }

        private void tryMergeLeft(Router router, RealTicket realTicket)
        {
            RealTicket lTicket = realTicket.left;
            if (lTicket != head && lTicket.isOkToUse()) {
                realTicket.left = lTicket.left;
                realTicket.departure = lTicket.departure;
                lTicket.left.right = realTicket;
                //router.freeTickets[router.computeIndex(lTicket.departure, lTicket.arrival)].decTicketNum();
                router.freeTickets[router.computeIndex(lTicket.departure, lTicket.arrival)].updateNum(-1);
            }
        }

        private void tryMergeRight(Router router, RealTicket realTicket)
        {
            RealTicket rTicket = realTicket.right;
            if (rTicket != head && rTicket.isOkToUse()) {
                realTicket.right = rTicket.right;
                realTicket.arrival = rTicket.arrival;
                rTicket.right.left = realTicket;
                //router.freeTickets[router.computeIndex(rTicket.departure, rTicket.arrival)].decTicketNum();
                router.freeTickets[router.computeIndex(rTicket.departure, rTicket.arrival)].updateNum(-1);
            }
        }

        public void trySplitRealTicket(Router router, WqfTicket.RealTicket realTicket, int departure, int arrival)
        {
            if (realTicket.departure < departure) {
                WqfTicket.RealTicket lTicket = new RealTicket(this, null, realTicket, //realTicket.left, realTicket,
                                                    realTicket.departure, departure);
                synchronized (this) {
                    lTicket.left = realTicket.left;
                    realTicket.left.right = lTicket;
                    realTicket.left = lTicket;
                    tryMergeLeft(router, lTicket);
		}
                router.freeTickets[router.computeIndex(lTicket.departure, lTicket.arrival)].push(lTicket);
            }

            if (arrival < realTicket.arrival) {
                WqfTicket.RealTicket rTicket = new RealTicket(this, realTicket, null, //realTicket.right,
                                                    arrival, realTicket.arrival);
                synchronized (this) {
                    rTicket.right = realTicket.right;
                    realTicket.right.left = rTicket;
                    realTicket.right = rTicket;
                    tryMergeRight(router, rTicket);
                }
                router.freeTickets[router.computeIndex(rTicket.departure, rTicket.arrival)].push(rTicket);
            }
        }

        synchronized public RealTicket findRealTicket(long tid)
        {
            RealTicket realTicket = head.right;
            while (realTicket != head) {
                if (realTicket.tid == tid)
                    return realTicket;
                realTicket = realTicket.right;
            }
            return null;
        }

        synchronized public void tryMergeRealTicket(Router router, RealTicket realTicket)
        {
            // has been mearged...
            if (realTicket.isIndividualAndFree.get() == false)
                return ;

            /*
            RealTicket lTicket = realTicket.left;
            if (lTicket != head && lTicket.isOkToUse()) {
                realTicket.left = lTicket.left;
                realTicket.departure = lTicket.departure;
                lTicket.left.right = realTicket;
                router.freeTickets[router.computeIndex(lTicket.departure, lTicket.arrival)].updateNum(-1);
            }

            RealTicket rTicket = realTicket.right;
            if (rTicket != head && rTicket.isOkToUse()) {
                realTicket.right = rTicket.right;
                realTicket.arrival = rTicket.arrival;
                rTicket.right.left = realTicket;
                router.freeTickets[router.computeIndex(rTicket.departure, rTicket.arrival)].updateNum(-1);
            }
             */
            tryMergeLeft(router, realTicket);
            tryMergeRight(router, realTicket);
        }
    }
}
