package ticketingsystem;
import java.util.concurrent.atomic.AtomicReference;

import java.util.concurrent.atomic.AtomicInteger;

public class WqfStack {
    //static final WqfTicket.RealTicket timeOutFlag = new WqfTicket.RealTicket(null, null, null, -1, -1);

    /*
    class Node {
        WqfTicket.RealTicket value;
        Node next;

        public Node(WqfTicket.RealTicket val)
        {
            value = val;
            next = null;
        }
    }

    class LockFreeExchanger {
        static final int EMPTY = 0;
        static final int WAITTING = 1;
        static final int BUSY = 2;
        AtomicStampedReference<WqfTicket.RealTicket> slot = new AtomicStampedReference(null, EMPTY);

        // if timeout we can chage another slot!!!
        // myItem != null, we are pushing
        // we are poping, otherwise.
        public WqfTicket.RealTicket exchange(WqfTicket.RealTicket myItem, int duration) {
            int clk;
            WqfTicket.RealTicket yrItem;

            clk = 0;
            int[] stampHolder = {EMPTY};
            while(clk++ < duration) {
                yrItem = slot.get(stampHolder);
                switch (stampHolder[0]) {
                    case EMPTY:
                        if (slot.compareAndSet(yrItem, myItem, EMPTY, WAITTING)) {
                            while (clk++ < duration) {
                                yrItem = slot.get(stampHolder);
                                if (stampHolder[0] == BUSY) {
                                    slot.set(null, EMPTY);
                                    return yrItem;
                                }
                            }

                            if (slot.compareAndSet(myItem, null, WAITTING, EMPTY))
                                return timeOutFlag;; // means time-out
                            yrItem = slot.get(stampHolder);
                            slot.set(null, EMPTY);
                            return yrItem;
                        }
                        break;
                    case WAITTING:
                        if (slot.compareAndSet(yrItem, myItem, WAITTING, BUSY))
                            return yrItem;
                        break;
                    case BUSY:
                        break;
                    default: ;// impossibly
                }
            }
            return timeOutFlag;
        }
    }

    class EliminationArray {
        static final int duration = 25;

        int capacity;
        Random random;
        LockFreeExchanger[] exchangers;

        public EliminationArray(int mCapacity)
        {
            capacity = mCapacity;
            random = new Random();
            exchangers = new LockFreeExchanger[capacity];
            for (int i = 0; i < capacity; i++)
                exchangers[i] = new LockFreeExchanger();
        }

        // push -> value != null
        // pop -> value == null
        public WqfTicket.RealTicket visit(WqfTicket.RealTicket value)
        {
            int index = random.nextInt(capacity);
            return exchangers[index].exchange(value, duration);
        }
    }
     */

    //EliminationArray eliminationArray;
    private volatile int num;
    //private AtomicInteger num;
    
    AtomicReference<WqfTicket.RealTicket> top = new AtomicReference<WqfTicket.RealTicket>(null);

    public WqfStack()//int capacity)
    {
        //num =  new AtomicInteger(0);
	num = 0;
    }

    synchronized void updateNum(int off)
    {
        num += off;
    }

    /*
    void incTicketNum() { num.getAndIncrement(); }
    void decTicketNum() { num.getAndDecrement(); }
    */

    boolean tryPush(WqfTicket.RealTicket node)
    {
        WqfTicket.RealTicket oldTop = top.get();
        node.next = oldTop;
        return top.compareAndSet(oldTop, node);
    }

    WqfTicket.RealTicket tryPop()
    {
        WqfTicket.RealTicket oldTop = top.get();
        if (oldTop == null)
            return null;
        WqfTicket.RealTicket newTop = oldTop.next;
        if (top.compareAndSet(oldTop, newTop))
            return oldTop;
        return null;
    }

    public void push(WqfTicket.RealTicket value)
    {
        updateNum(1);
        //incTicketNum();
        while (true) {
            if (tryPush(value))
                break;
        }
    }

    public void pushWithoutExchange(WqfTicket.RealTicket value)
    {
        updateNum(1);
        //incTicketNum();
        while (!tryPush(value))
            continue;
    }

    public WqfTicket.RealTicket pop()
    {
        while (top.get() != null) {
            WqfTicket.RealTicket resNode = tryPop();
            if (resNode != null && resNode.isOkToUse()) {
                updateNum(-1);
		//decTicketNum();
                return resNode;
            }
        }
        return null;
    }

    public int getNumOfFreeTickets()
    {
        /// i accept num < 0 exists because of serialization consistent....
        /// if no further buy/refund, num will be >= 0 at last...
        //int localNum = num.get();
        int localNum = num;
        return (localNum<0)?0:localNum;
    }
}
