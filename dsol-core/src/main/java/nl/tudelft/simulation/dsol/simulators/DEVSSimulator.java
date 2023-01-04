package nl.tudelft.simulation.dsol.simulators;

import java.io.Serializable;

import org.djutils.exceptions.Throw;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.eventlists.EventListInterface;
import nl.tudelft.simulation.dsol.eventlists.RedBlackTree;
import nl.tudelft.simulation.dsol.experiment.ReplicationInterface;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.Executable;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.LambdaSimEvent;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEvent;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;
import nl.tudelft.simulation.dsol.model.DSOLModel;
import nl.tudelft.simulation.dsol.simtime.SimTime;

/**
 * The DEVS defines the interface of the DEVS simulator. DEVS stands for the Discrete Event System Specification. More
 * information on Discrete Event Simulation can be found in "Theory of Modeling and Simulation" by Bernard Zeigler et.al.
 * <p>
 * Copyright (c) 2002-2023 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://simulation.tudelft.nl/" target="_blank"> https://simulation.tudelft.nl</a>. The DSOL
 * project is distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://https://simulation.tudelft.nl/dsol/docs/latest/license.html" target="_blank">
 * https://https://simulation.tudelft.nl/dsol/docs/latest/license.html</a>.
 * </p>
 * @author <a href="https://www.linkedin.com/in/peterhmjacobs">Peter Jacobs </a>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @param <T> the simulation time type based on the absolute and relative time.
 * @since 1.5
 */
public class DEVSSimulator<T extends Number & Comparable<T>> extends Simulator<T> implements DEVSSimulatorInterface<T>
{
    /** */
    private static final long serialVersionUID = 20140804L;

    /** eventList represents the future event list. */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected EventListInterface<T> eventList = new RedBlackTree<T>();

    /**
     * Constructs a new DEVSSimulator.
     * @param id the id of the simulator, used in logging and firing of events.
     */
    public DEVSSimulator(final Serializable id)
    {
        super(id);
    }

    /** {@inheritDoc} */
    @Override
    public boolean cancelEvent(final SimEventInterface<T> event)
    {
        return this.eventList.remove(event);
    }

    /** {@inheritDoc} */
    @Override
    public EventListInterface<T> getEventList()
    {
        return this.eventList;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"hiding", "checkstyle:hiddenfield"})
    public void initialize(final DSOLModel<T, ? extends SimulatorInterface<T>> model, final ReplicationInterface<T> replication)
            throws SimRuntimeException
    {
        // this check HAS to be done BEFORE clearing the event list
        Throw.when(isStartingOrRunning(), SimRuntimeException.class, "Cannot initialize a running simulator");
        synchronized (super.semaphore)
        {
            this.eventList.clear();
            super.initialize(model, replication);
            this.scheduleEvent(new SimEvent<T>(this.getReplication().getWarmupSimTime(),
                    (short) (SimEventInterface.MAX_PRIORITY + 1), this, "warmup", null));
            this.scheduleEvent(new SimEvent<T>(this.getReplication().getEndSimTime(),
                    (short) (SimEventInterface.MIN_PRIORITY - 1), this, "endReplication", null));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEvent(final SimEventInterface<T> event) throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            if (event.getAbsoluteExecutionTime().compareTo(super.simulatorTime) < 0)
            {
                throw new SimRuntimeException("cannot schedule event " + event.toString() + " in past " + this.simulatorTime
                        + ">" + event.getAbsoluteExecutionTime());
            }
            this.eventList.add(event);
            return event;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventRel(final T relativeDelay, final short priority, final Object target,
            final String method, final Object[] args) throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            T absEventTime = SimTime.plus(this.simulatorTime, relativeDelay);
            return scheduleEvent(new SimEvent<T>(absEventTime, priority, target, method, args));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventRel(final T relativeDelay, final Object target, final String method,
            final Object[] args) throws SimRuntimeException
    {
        return scheduleEventRel(relativeDelay, SimEventInterface.NORMAL_PRIORITY, target, method, args);
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventAbs(final T absoluteTime, final short priority, final Object target,
            final String method, final Object[] args) throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            return scheduleEvent(new SimEvent<T>(absoluteTime, priority, target, method, args));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventAbs(final T absoluteTime, final Object target, final String method,
            final Object[] args) throws SimRuntimeException
    {
        return scheduleEventAbs(absoluteTime, SimEventInterface.NORMAL_PRIORITY, target, method, args);
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventNow(final short priority, final Object target, final String method,
            final Object[] args) throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            T absEventTime = SimTime.copy(this.simulatorTime);
            return scheduleEvent(new SimEvent<T>(absEventTime, priority, target, method, args));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventNow(final Object target, final String method, final Object[] args)
            throws SimRuntimeException
    {
        return scheduleEventNow(SimEventInterface.NORMAL_PRIORITY, target, method, args);
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventRel(final T relativeDelay, final short priority, final Executable executable)
            throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            T absEventTime = SimTime.plus(this.simulatorTime, relativeDelay);
            return scheduleEvent(new LambdaSimEvent<T>(absEventTime, priority, executable));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventRel(final T relativeDelay, final Executable executable) throws SimRuntimeException
    {
        return scheduleEventRel(relativeDelay, SimEventInterface.NORMAL_PRIORITY, executable);
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventAbs(final T absoluteTime, final short priority, final Executable executable)
            throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            return scheduleEvent(new LambdaSimEvent<T>(absoluteTime, priority, executable));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventAbs(final T absoluteTime, final Executable executable) throws SimRuntimeException
    {
        return scheduleEventAbs(absoluteTime, SimEventInterface.NORMAL_PRIORITY, executable);
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventNow(final short priority, final Executable executable) throws SimRuntimeException
    {
        synchronized (super.semaphore)
        {
            T absEventTime = SimTime.copy(this.simulatorTime);
            return scheduleEvent(new LambdaSimEvent<T>(absEventTime, priority, executable));
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimEventInterface<T> scheduleEventNow(final Executable executable) throws SimRuntimeException
    {
        return scheduleEventNow(SimEventInterface.NORMAL_PRIORITY, executable);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void setEventList(final EventListInterface<T> eventList)
    {
        this.eventList = eventList;
        this.fireEvent(EVENTLIST_CHANGED_EVENT);
    }

    /** {@inheritDoc} */
    @Override
    protected void stepImpl()
    {
        synchronized (super.semaphore)
        {
            if (!this.eventList.isEmpty())
            {
                SimEventInterface<T> event = this.eventList.removeFirst();
                fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, event.getAbsoluteExecutionTime());
                super.simulatorTime = event.getAbsoluteExecutionTime();
                event.execute();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        // set the run flag semaphore to signal to startImpl() that the run method has started
        this.runflag = true;
        while (!isStoppingOrStopped())
        {
            synchronized (super.semaphore)
            {
                int cmp = this.eventList.isEmpty() ? 2
                        : this.eventList.first().getAbsoluteExecutionTime().compareTo(this.runUntilTime);
                if ((cmp == 0 && !this.runUntilIncluding) || cmp > 0)
                {
                    this.simulatorTime = SimTime.copy(this.runUntilTime);
                    this.runState = RunState.STOPPING;
                    break;
                }

                SimEventInterface<T> event = this.eventList.removeFirst();
                if (event.getAbsoluteExecutionTime().compareTo(super.simulatorTime) != 0)
                {
                    fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, event.getAbsoluteExecutionTime());
                }
                super.simulatorTime = event.getAbsoluteExecutionTime();
                try
                {
                    event.execute();
                }
                catch (Exception exception)
                {
                    handleSimulationException(exception);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endReplication()
    {
        super.endReplication();
        this.eventList.clear();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public boolean isPauseOnError()
    {
        return getErrorStrategy().equals(ErrorStrategy.WARN_AND_PAUSE);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void setPauseOnError(final boolean pauseOnError)
    {
        setErrorStrategy(pauseOnError ? ErrorStrategy.WARN_AND_PAUSE : ErrorStrategy.LOG_AND_CONTINUE);
    }

}
