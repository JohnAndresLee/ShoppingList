package SL;

import java.io.*;
import java.util.*;

//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import javax.swing.plaf.synth.SynthDesktopIconUI;

public class ShoppingList {
    static class Map extends Mapper<Object, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private int k;


        public List<List<String>> combinations(List<String> c, int ke){
            if(ke>c.size()){
                return new ArrayList<List<String>>();
            }
            List<List<String>> ans = new ArrayList<List<String>>();
            if(ke==1){
                for(String ci:c){
                    List<String> ttt = new ArrayList<String>();
                    ttt.add(ci);
                    ans.add(ttt);
                }
                return ans;
            }
            String first = c.get(0);
            List<String> c_copy = new ArrayList<String>();
            c_copy.addAll(c);
            c_copy.remove(0);
            List<List<String>> tmp = combinations(c_copy,ke-1);
            for(List<String> t:tmp){
                t.add(0, first);
            }
            ans.addAll(tmp);
            List<List<String>> tmp2 = combinations(c_copy, ke);
            ans.addAll(tmp2);
            return ans;
        }

        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString();
            StringTokenizer itr = new StringTokenizer(line);
            Set<String> ss = new HashSet<String>();
            while (itr.hasMoreTokens()) {
                ss.add(itr.nextToken());
            }
            List<String> commodity = new ArrayList<String>(ss);
            Collections.sort(commodity);
            List<List<String>> coms = combinations(commodity, k);
            for (List<String> com: coms){
                context.write(new Text(com.toString()), one);
            }
        }

        public void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            k = conf.getInt("k", 2);
        }
    }

    public static class  MyRecorderWriter extends RecordWriter<IntWritable, Text>{
        private FSDataOutputStream out;

        public MyRecorderWriter(TaskAttemptContext job) throws IOException {
            String outdoor = job.getConfiguration().get(FileOutputFormat.OUTDIR);
            FileSystem fileSystem = FileSystem.get(job.getConfiguration());
            out = fileSystem.create(new Path(outdoor + "/hw7_out.txt"));
        }
        @Override
        public void write(IntWritable key, Text value) throws IOException, InterruptedException {
            out.writeBytes(value.toString());
            out.writeBytes(String.valueOf(key.get()));
            out.write('\n');
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException, InterruptedException {
            out.close();
        }
    }

    public  static class MyOutputFormat extends FileOutputFormat<IntWritable, Text>{
        @Override
        public RecordWriter<IntWritable, Text> getRecordWriter(TaskAttemptContext job) throws IOException, InterruptedException {
            MyRecorderWriter myRecorderWriter = new MyRecorderWriter(job);
            return myRecorderWriter;
        }
    }

    //对value降序排序
    private static class IntWritableDecreasingComparator extends IntWritable.Comparator {
        public int compare(WritableComparable a, WritableComparable b) {
            return -super.compare(a, b);
        }

        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return -super.compare(b1, s1, l1, b2, s2, l2);
        }
    }

    static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable>{
        private IntWritable result = new IntWritable();
        private int min;

        protected void reduce(Text key, Iterable<IntWritable> values,
                              Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            if(result.get()>=min){
                String tt = key.toString();
                tt = tt.replace("[","(");
                tt = tt.replace("]","),");
                context.write(new Text(tt), result);
            }
        }

        public void setup(Reducer.Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            min = conf.getInt("min", 2);
        }

    }

    private static String inputPath = "/user/hadoop/hw7_input";
    private static String tmpDir = "/user/hadoop/hw7_tmp_output";
    private static String outputDir = "/user/hadoop/hw7_output";

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "shoppinglist");
        job.setJarByClass(ShoppingList.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);

        String[] remainingArgs = optionParser.getRemainingArgs();

        List<String> otherArgs = new ArrayList<String>();
        for (int i=0; i < remainingArgs.length; ++i) {
            otherArgs.add(remainingArgs[i]);
        }
        job.getConfiguration().setInt("k", Integer.parseInt(otherArgs.get(0)));
        job.getConfiguration().setInt("min", Integer.parseInt(otherArgs.get(1)));


        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileSystem fs = FileSystem.get(conf);
        Path inPath = new Path(inputPath);
        if (fs.exists(inPath)) {
            FileInputFormat.addInputPath(job, inPath);
        }

        if(fs.exists(new Path(outputDir))){
            fs.delete(new Path(outputDir), true);
        }

        Path tempDir = new Path(tmpDir);
        FileOutputFormat.setOutputPath(job, tempDir);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        if(job.waitForCompletion(true))
        {
            Job sortJob = new Job(conf, "sort");
            sortJob.setJarByClass(ShoppingList.class);

            FileInputFormat.addInputPath(sortJob, tempDir);
            sortJob.setInputFormatClass(SequenceFileInputFormat.class);

            /*InverseMapper由hadoop库提供，作用是实现map()之后的数据对的key和value交换*/
            sortJob.setMapperClass(InverseMapper.class);
            /*将 Reducer 的个数限定为1, 最终输出的结果文件就是一个。*/
            sortJob.setNumReduceTasks(1);

            FileOutputFormat.setOutputPath(sortJob, new Path(outputDir));

            sortJob.setOutputFormatClass(MyOutputFormat.class);

            sortJob.setOutputKeyClass(IntWritable.class);
            sortJob.setOutputValueClass(Text.class);

            sortJob.setSortComparatorClass(IntWritableDecreasingComparator.class);
            if(sortJob.waitForCompletion(true))//删除中间文件
                fs.delete(tempDir);
            System.exit(sortJob.waitForCompletion(true) ? 0 : 1);
        }
    }
}
