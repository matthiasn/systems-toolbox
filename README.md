# systems-toolbox

Applications are systems; however, don't take my word for it. Let's see how an expert on **Systems Thinking** defines a system:

> "A system isn't just any old collection of things. A system is an interconnected set of elements that is coherently organized in a way that achieves something. If you look at that definition closely for a minute, you can see that a system must consist of three kinds of things: elements, interconnections, and a function or purpose." - Meadows, Donatella H. (2008) Thinking in Systems: A Primer, Page 11

This applies to every meaningful application I've ever written. How do we get closer to understanding such a system? Again, here's a quote:

> "The behavior of a system is its performance over time--its growth, stagnation, decline, oscillation, randomness, or evolution." - Meadows, Donatella H. (2008) Thinking in Systems: A Primer, Page 88

Her remarks make sense. The code itself is just the blueprint for the system. Code is like the blueprint for a busy train station versus the actual train station. You won't see where, exactly, congestion will occur until you observe bottlenecks in the living system. We need to observe and monitor a running system to understand it better.

At the same time, when writing applications using **[core.async](https://github.com/clojure/core.async)**, I find myself dealing with building blocks time and time again that have little to do with the observable logic. I've seen this with **[BirdWatch](https://github.com/matthiasn/BirdWatch)**, **[inspect](https://github.com/matthiasn/inspect)**, or also an **AngularJS markup to Hiccup conversion tool** (not published). I repeatedly wrote **[channels](http://clojure.github.io/core.async/#clojure.core.async/chan)** and **[go-loops](http://clojure.github.io/core.async/#clojure.core.async/go-loop)**. These are just incidental complexity and orthogonal to what the application is trying to solve.

Instead, there should be more high-level building blocks that only handle incoming messages and potentially emit messages, as well. If you think that I am referring to the **[actor model](http://en.wikipedia.org/wiki/Actor_model)**, not so fast. Yes, **actors** have desirable properties, but I don't like that they need to know where to send messages.

So here's my idea: there are message switchboards that connect to components because we wire them inside the switchboard logic and route messages depending on the kind of message. Other than a namespaced keyword that describes the payload (potentially while checking the compliance with a schema), the switchboards do not care about the payload at all.

Switchboards then dispatch messages to the connected components. These components process messages, for example, by publishing a document to a database or answering a query or whatnot; or, such components could be additional, cascaded switchboards that, again, route messages to other components.

Message routing should be possible by matching namespaced keywords either exactly or with wildcard matches to allow for the utmost flexibility.

I want to start and wire components at compile time. I also want to fire up components, wire them in a switchboard, disconnect them, or shut them down during run time. A system is a living thing, so I should be able to modify its behavior whenever I feel like it.

Also, observability needs to be an integral part of the system from the first moment on, not as an afterthought. As we can learn from the quote above, a system expresses a behavior over time. Since we want to leverage this behavior to get a better insight into the system, what could be of interest? I think waiting times until a message is getting processed and processing time for each message are suitable candidates, as well as the development of these metrics over time. Also, once we know how long it took to process an individual message, we may also want to know what the message itself was. The system should harvest these data points by default. We don't necessarily need to persist every message, but at least recent messages should be available for close inspection. The number of these depends on the available resources at any given point in time.

Combine this with a built-in visualizer of the information flow. Since the structure of the application and its flow is nothing but data, we can take advantage of the **[SVG](http://en.wikipedia.org/wiki/Scalable_Vector_Graphics)** drawing capability of **[ReactJS](http://facebook.github.io/react/)** and **[Reagent](http://reagent-project.github.io)**. Any visualization always reflects the status quo of the structure of the system. When I fire up a new component at run time, this should be reflected immediately. Then, for each visualized component, there are gauges and charts that display how the components behave, both now and in the past. Also, the user interface displays incoming and outflowing data structures as desired.

These observation tools should put us in an excellent place for understanding a running system by observing its behavior. Then, we can learn more about our systems, both under real load and under simulated load, and determine where, exactly, additional effort is well spent.

Components could, for example, as already suggested, take care of database lookups. Also, they could provide a bi-directional connection between client and server over a WebSockets connection. Yet another kind of component could facilitate communication between two JVMs, e.g. using **[Kafka](http://kafka.apache.org)**, **[RabbitMQ](http://www.rabbitmq.com)**, **[Redis](http://redis.io)**, or **[HornetQ](http://hornetq.jboss.org)**.

Other components encapsulate application state and surrounding business logic. The only way to interact with the application state is via messages, where it is entirely up to the state handling logic how to deal with those messages. Inside, the state is kept in an atom but this atom is not freely passed around. Only the dereferenced application state is sent back to the connected switchboard when a change occurs.

Other components can render the received data as HTML using ReactJS and Reagent and emit messages back to the Switchboard when the user clicks a button, or when any other kind of interaction with the UI occurs. The state handling components can then react to the event, or the switchboard forwards a query to the server; this totally depends on how we wire up the switchboard for the particular message type.
