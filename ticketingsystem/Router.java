package ticketingsystem;

public class Router {
    //final static int exchangerCapacity = 3;

    private int stationNum;
    private int coachs, seats;

    WqfStack[] freeTickets;
    private WqfTicket.LogicTicket[] LTickets;
    //private CombiningTree tidgenerator;

    public int computeIndex(int d, int a)
    {
        return (((d-1)*((stationNum<<1)-d))>>1)+a-d-1;
    }

    private boolean isUnvalidCoachSeat(int coach, int seat)
    {
        return (coach < 0) | (coach > coachs) | (seat < 0) | (seat > seats);
    }

    //public Router(CombiningTree mTidgenerator, int cn, int sn, int mStationNum)
    public Router(int cn, int sn, int mStationNum)
    {
        coachs = cn; seats = sn;
        stationNum = mStationNum;
        //tidgenerator = mTidgenerator;

        int stackNum = ((stationNum)*(stationNum-1))>>1;
        freeTickets = new WqfStack[stackNum];
        for (int i = 0; i < stackNum; i++)
            freeTickets[i] = new WqfStack();//exchangerCapacity);

        int base = 0;
        int initIdx = computeIndex(1, stationNum);
        LTickets = new WqfTicket.LogicTicket[cn*sn];

        for (int i = 1; i <= cn; i++) {
            for (int j = 1; j <= sn; j++) {
                LTickets[base] = new WqfTicket.LogicTicket(i, j);
                freeTickets[initIdx].pushWithoutExchange(LTickets[base].initRealTicket(stationNum));
                ++base;
            }
        };
    }

    private WqfTicket.RealTicket findRealTicketBestMatch(int departure, int arrival)
    {
        int d, a;
        WqfTicket.RealTicket rTicket;
        // first stage...
        // searching (d, a) -> (d-1, a) -> (d, a+1) -> ...
        while ((departure >= 1) && (arrival <= stationNum)) {
            d = (departure-1>=1)?(departure-1):1;
            a = (arrival+1<=stationNum)?(arrival+1):stationNum;
            rTicket = freeTickets[computeIndex(departure, arrival)].pop();
            if (rTicket != null) return rTicket;
            if (d != departure) {
                rTicket = freeTickets[computeIndex(d, arrival)].pop();
                if (rTicket != null)  return rTicket;
            }
            if (a != arrival) {
                rTicket = freeTickets[computeIndex(departure, a)].pop();
                if (rTicket != null) return rTicket;
            }
            departure--;
            arrival++;
        }
        // second stage...
        if (departure >= 1) {
            departure--;
            while (departure > 0) {
                rTicket = freeTickets[computeIndex(departure--, stationNum)].pop();
                if (rTicket != null)
                    return rTicket;
            }
        }
        if (arrival <= stationNum) {
            int baseIdx = computeIndex(1, ++arrival);
            while (arrival++ <= stationNum) {
                rTicket = freeTickets[baseIdx++].pop();
                if (rTicket != null)
                    return rTicket;
            }
        }
        return null;
    }

    private WqfTicket.RealTicket findRealTicketLeastMatch(int departure, int arrival)
    {
        WqfTicket.RealTicket realTicket;

        for (int i = 1; i <= departure; i++) {
            int baseIdx = computeIndex(i, stationNum);
            for (int j = stationNum; j >= arrival; j--) {
                //System.out.println(baseIdx);
                realTicket = freeTickets[baseIdx--].pop();
                if (realTicket != null)
                    return realTicket;
            }
        }
        return null;
    }

    private WqfTicket.RealTicket findTicket(int departure, int arrival)
    {
        int baseIdx = computeIndex(departure, arrival);
        WqfTicket.RealTicket realTicket;

        realTicket = freeTickets[baseIdx].pop();
        if (realTicket != null)
            return realTicket;
        //}
        // least match but more cache-friendly...
        realTicket = findRealTicketLeastMatch(departure, arrival);
        if (realTicket != null)
            return realTicket;
        // best match but cache-unfriendly...
        realTicket = findRealTicketBestMatch(departure, arrival);
        if (realTicket != null)
            return realTicket;
        //}
        return null;
    }

    private Ticket fillInTicket(WqfTicket.RealTicket rTicket)
    {
        Ticket ticket = new Ticket();
        ticket.tid = rTicket.tid;
        ticket.departure = rTicket.departure;
        ticket.arrival = rTicket.arrival;
        ticket.passenger = rTicket.passenger;
        ticket.coach = rTicket.root.coachId;
        ticket.seat = rTicket.root.seatId;
        return ticket;
    }

    public Ticket buyTicket(String passenger, int departure, int arrival)
    {
        if (arrival > stationNum)
            return null;
        //Random rand = new Random();
        Ticket ticket = null;
        WqfTicket.RealTicket rTicket;
        rTicket = findTicket(departure, arrival);

        if (rTicket != null) {
            rTicket.root.trySplitRealTicket(this, rTicket, departure, arrival);
            rTicket.passenger = passenger;
            //rTicket.tid = tidgenerator.getAndIncrement();
            rTicket.tid = TidGenerator.getTid();
            rTicket.departure = departure;
            rTicket.arrival = arrival;
            ticket = fillInTicket(rTicket);
        }
        return ticket;
    }

    public int inquiry(int departure, int arrival)
    {
        if (arrival > stationNum)
            return 0;

        int sum = 0;
        for (int i = 1; i <= departure; i++) {
            int baseIdx = computeIndex(i, stationNum);
            for (int j = stationNum; j >= arrival; j--)
                sum += freeTickets[baseIdx--].getNumOfFreeTickets();
        }
        return sum;
    }

    private boolean isTwoTicketsMatch(Ticket ticket, WqfTicket.RealTicket realTicket)
    {
        return (ticket.passenger != null)
                & ticket.passenger.equals(realTicket.passenger)
                & (ticket.departure == realTicket.departure)
                & (ticket.arrival == realTicket.arrival);
    }

    public boolean refundTicket(Ticket ticket)
    {
        if ((ticket.departure >= stationNum) || (ticket.arrival > stationNum)
                || isUnvalidCoachSeat(ticket.coach, ticket.seat))
            return false;
        int idx = (ticket.coach-1)*seats+ticket.seat-1;
        WqfTicket.LogicTicket LTicket = LTickets[idx];
        WqfTicket.RealTicket realTicket = LTicket.findRealTicket(ticket.tid);
        if ((realTicket != null) && isTwoTicketsMatch(ticket, realTicket)) {
            realTicket.tid = -1;
            realTicket.isIndividualAndFree.set(true);
            realTicket.root.tryMergeRealTicket(this, realTicket);
            idx = computeIndex(realTicket.departure, realTicket.arrival);
            freeTickets[idx].push(realTicket);
            return true;
        }

        return false;
    }
}
