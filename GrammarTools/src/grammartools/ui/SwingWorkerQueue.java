package grammartools.ui;

import java.util.LinkedList;
import java.util.Queue;
import javax.swing.SwingWorker;

public class SwingWorkerQueue
{
    /**
     * A job to be queued and later run on a SwingWorker.
     */
    public interface Job
    {
        /**
         * Called on the Swing thread before starting the job.
         */
        void start();
        /**
         * Called in a background thread to perform the job.
         */
        void perform();
        /**
         * Called on the Swing thread after completing the job.
         */
        void complete();
    }
        
    public void submit(final Job job)
    {
        queue.add(job);
        
        if(queue.size() == 1)
        {
            update();
        }
    }
    
    private void update()
    {
        if(queue.isEmpty())
        {
            return;
        }
        
        final Job job = queue.peek();
        
        new SwingWorker<Object, Object>()
        {
            public void start()
            {
                job.start();
                execute();
            }
            @Override protected Object doInBackground()
            {
                job.perform();
                return null;
            }
            @Override protected void done()
            {
                job.complete();
                queue.remove();
                update();
            }
        }.start();
    }

    private final Queue<Job> queue = new LinkedList<Job>();
}
