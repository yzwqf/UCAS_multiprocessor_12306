package ticketingsystem;

import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    Router[] routers;
    //CombiningTree tidgenerator;

    TicketingDS(int rn, int cn, int sn, int sttnum, int tn) throws InterruptedException {
        //tidgenerator = new CombiningTree(tn, 0);

        TidGenerator.setThreadNum(tn);
        routers = new Router[rn];
        for (int i = 0; i < rn; i++)
            routers[i] = new Router(cn, sn, sttnum);
            //routers[i] = new Router(tidgenerator, cn, sn, sttnum);
    }

    private boolean isUnvalidDepartureAndArrival(int d, int a)
    {
        return (d >= a) | (d <= 0 || a <= 0);
    }

    private boolean isValidTicket(Ticket ticket)
    {
        return !((ticket == null) | (ticket.route <= 0 |  ticket.route > routers.length));
    }

    public Ticket buyTicket(String passenger, int route, int departure, int arrival)
    {
        if (route <= 0 || route > routers.length || isUnvalidDepartureAndArrival(departure, arrival))
            return null;
        Ticket ticket = routers[route-1].buyTicket(passenger, departure, arrival);
        if (ticket != null)
            ticket.route = route;
        return ticket;
    }

    public int inquiry(int route, int departure, int arrival)
    {
        if (isUnvalidDepartureAndArrival(departure, arrival))
            return 0;
        return routers[route-1].inquiry(departure, arrival);
    }

    public boolean refundTicket(Ticket ticket)
    {
        if (isValidTicket(ticket) && !isUnvalidDepartureAndArrival(ticket.departure, ticket.arrival))
            return routers[ticket.route-1].refundTicket(ticket);
        return false;
    }
}
