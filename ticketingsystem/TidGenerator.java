package ticketingsystem;

public class TidGenerator {
    static int threadNum ;

    public static void setThreadNum(int n) { threadNum = n; }

    private static ThreadLocal<Long> tid = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return Long.valueOf(WqfThreadID.get());
        }};

    public static long getTid()
    {
        long currTid = tid.get().longValue();
        tid.set(Long.valueOf(currTid + threadNum));
        return currTid;
    }
/*
    static int threadNum ;
    static long[] threadTids;

    public static void setThreadNum(int n)
    {
        threadNum = n;
        threadTids = new long[threadNum];
        for (int i = 0; i < threadTids.length; i++)
            threadTids[i] = -1;
    }

    private static ThreadLocal<Long> tid = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return Long.valueOf(WqfThreadID.get());
        }};

    public static long getTid()
    {
        int threadId = WqfThreadID.get();
        if (threadTids[threadId] == -1)
            threadTids[threadId] = threadId;
        long currTid = threadTids[threadId];
        threadTids[threadId] += threadNum;
        return currTid;
    }
*/
}
