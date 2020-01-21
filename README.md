# 性能评测报告
## 负载测试结果
&emsp;&emsp;下表为分别以1, 16, 32, 64和96线程运行本程序所消耗时间, 输入参数为20个车次，每个车次15节车厢，每节车厢100个座位，途径10个车站，各个线程执行500000次操作，其中查询，购买和退票的比例分别为80%，15%和5%。</br>

线程数  |	1   | 	16   |	32   |	64   |	96
--------|------|--------|-------|-------|-----
时间/秒  |	0.230305771   |	0.937675465  |	1.315433571  |	2.275832332  |	3.213430391
操作/秒  |	2171026.8     |	8531736.5    |	12163290.0   |	14060789.8   |	14937308.2

## 设计思路
&emsp;&emsp;各个类实现了相对独立的功能，因此将以类为单位进行剖析。
### WqfThreadID:
&emsp;&emsp;为每个线程生成全局唯一且恒不变的线程ID, 调用类方法WqfThreadID.get()即可获取。<br>
&emsp;&emsp;注：线程ID的取值区间为[0, N-1], 其中N为系统中的线程总数。<br>
### TidGenerator
&emsp;&emsp;调用类方法getTid()可生成全局唯一的Ticket.tid, 因为对tid的要求是“每张车票都有一个唯一的编号tid,不能重复”，无需考虑连续性，也就是说假设x已分配出去, 尽管未分配x+1, 却仍可以分配tid>x+1。<br>
&emsp;&emsp;因此我的做法是让每个线程维护一个与其线程ID相关的增长序列。具体地说,假设系统中功能N个线程, 某线程的线程ID为x, 则该线程生成的tid只能是{x, x+N, x+2N, … , x+kN}。由线程ID的唯一性和线程ID<N(系统中线程数)易得tid的唯一性，证明略。<br>
&emsp;&emsp;本方法生成的tid是同线程内递增的, 但不保证线程之间有序。暂不考虑溢出的情况(本实验也不会溢出Long)。<br>
&emsp;&emsp;基于上述思考，可用ThreadLocal<Long>来维护线程的tid序列。故每次产生新的tid不会要求各线程间同步，提高了效率，经测量在96线程情况下可提高1秒多。
	
### WqfTicket
#### RealTicket
&emsp;&emsp;该类是我的实现中真正的“票”。每个RealTicket对象对应于一个合法的TicketingSystem.Ticket对象, 结构与Ticket类似，区别在于没有coach和seat域(这两个域由LogicTicket维护, 接下来会阐述二者关系)，增加了同为RealTicket类引用的left, right和next域。其中left, right用于与其他RealTicket对象融合和分裂, next对象用于栈操作中。增加了root域指向所属的LogicTicket对象。<br>
&emsp;&emsp;IsOkToUse(): lock-free, 判断该对象是否空闲，通过AtomicBoolean实现。

#### LogicTicket
&emsp;&emsp;每躺车次的每个座位对应一个LogicTicket对象, 具体地说，若有coach节车厢, 每节车厢有seat个座位, 则该趟车次的逻辑结构中有coach\*seat个该类型的对象。其coachId和seatId两个域指明了该对象对应座位在本次列车中的相对位置，其中coachId取值范围是\[0, coach-1], seatId取值范围是\[0, seat-1]。<br>
&emsp;&emsp;包含RealTicket类型的对象head, 作用是作为哨兵节点，与其他RealTicket对象中的left和right共同构成双向链表。双向链表的作用是维护该座位卖出的全部票以及可售的票。具体地说，若该座位已售出a->b的票, 那么b->c的票仍然是可售的。<br>
&emsp;&emsp;上文介绍RealTicket时所说的分裂意思是，若想买的区间是a->b, 查询该座位时发现a->c都是可售的, 那么本系统将会只卖出a->b的票, 把b->c的票保留, 以供他人购买。具体到程序里就是把对应的RealTicket分为两个独立的个体, 一个标记起始站为a,终点站是b，然后用于购票流程；另一个起始为b，终点站为c，状态为空闲，并加入Router中对应的栈里(见下文)。<br>
&emsp;&emsp;融合则是用于退票的时候, 当对应于a->b的RealTicket对象进入退票逻辑时, 通过其root域找到所属的LogicTicket对象, 发现b->c也空闲的时候, 就将两个RealTicket融合成对应a->c区间的新RealTicket对象, 增加售出票的可能。<br>
&emsp;&emsp;对上述双向链表的访问都通过synchronized修饰的方法, 保证了互斥性，且可线性化，线性化点在于对象锁的获得。<br>
&emsp;&emsp;以下方法通过synchronized实现lock-free:<br> 
&emsp;&emsp;&emsp;&emsp;findRealTicket,  tryMergeRealTicket,  trySplitRealTicket。

### WqfStack
&emsp;&emsp;成员变量top的是以RealTicket为值的lock-free的栈，类型为AtomicReferen-ce<RealTicket>,compareAndSet()是push()和pop()的可线性化点。对空栈调用pop()不会阻塞, 而是返回null表示栈为空。<br>
&emsp;&emsp;使用被volatile关键字修饰的变量num追踪栈中元素个数。对其修改通过updateNum方法完成,只能是增加或减少1。该方法被synchronized关键字修饰。可通过getNumOfFreeTickets方法来获取num的值。<br>
&emsp;&emsp;注: num的值在某时刻中可能小于0, 表示当前数值不稳定, 但最终会回到0或以上，在没有线程执行购票或退票的静止状态下, 该值一定大于等于0。getNumOfFreeTickets方法在遇到num小于0时, 会返回0.<br>
	
### Router
&emsp;&emsp;每趟车次对应一个Router对象, 用于处理涉及相关查询，买票和退票请求，本系统中一张票不可能涉及两趟车次，所以对各车次的请求分开来处理。<br>
&emsp;&emsp;主要数据结构是LogicTicket对象的数组，大小为coach\*seat,即本车次中全体座位数。每个对象对应一个座位。退票时根据传入的Ticket对象的coach和seat锁定其对应的LogicTicket对象, 遍历以head域开始的双向链表，检测是否有对应的票，并执行退票逻辑。<br>
&emsp;&emsp;元素为WqfStack对象的数组, 变量名为freeTickets, 该数组的长度是依据如下事实计算的:<br>
&emsp;&emsp;&emsp;&emsp;**本列车经过的各站编号为1,2…,N。那么只存在1->2, 1->3,…,1->N的合法区间, 不考虑i->1(i>1)这样的售票区间。即售票区间只能是终点站的编号大于始发站编号。那么总共有N(N+1)/2种售票区间。**<br>
&emsp;&emsp;售票区间总数即为该数组的大小。ComputeIndex方法接收两个参数,分别为始发站和终点站编号, 返回相应区间对应的空闲票栈在freeTickets种的下标。<br>
&emsp;&emsp;买票是buyTicket方法实现的, 假设始发站和终点站分别为d,a。我的买票策略是先调用computeIndex(d,a)计算出对应栈在freeTickets中的下标, 调用该栈的pop()方法, 若返回值非空, 则找到余票, 然后填入相关信息并进入后续流程。若返回值为null, 表示目前该区间内不存在余票, 则尝试所有包含该区间的购票区间对应的栈。已经实现了LogicTicket的融合和分裂逻辑, 所以只要对所有购票区间[i,j](1<=i<=d, j<=a<=N)对应的栈调用pop()并判断即可。若上述全都检索完依旧没有余票，则返回null。<br>
&emsp;&emsp;Inquiry方法用于查询余票。遍历包含d->a的所有区间对应的freeTickets中的元素, 调用getNumOfFreeTickets返回值之和即为当前余票。<br>
&emsp;&emsp;refundTicket用于退票。根据route, coach和seat唯一确定LogicTicket对象, 遍历该对象里的双向链表(以head为头节点, 元素类型为RealTicket,见上文), 若存在tid, passenger，depature和arrival全部匹配的元素, 则登记该RealTicket对象为空闲状态并尝试和相邻对象合并。<br>
&emsp;&emsp; **Router类方法全部依赖于LogicTicket, RealTicket和WqfStack的并发机制，以此保证了正确性和可线性化。** 
