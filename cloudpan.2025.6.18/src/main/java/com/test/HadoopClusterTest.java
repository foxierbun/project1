package com.test;
//测试类
//Hadoop 配置类，用于加载 Hadoop 集群的配置参数（如 HDFS 地址、端口等）
import org.apache.hadoop.conf.Configuration;
//HDFS 文件系统的核心操作类（如创建目录、读写文件）
import org.apache.hadoop.fs.*;
//输入输出异常类（HDFS 操作可能抛出此异常）
import java.io.IOException;
//字符集枚举类（指定文本编码，如 UTF-8）
import java.nio.charset.StandardCharsets;

//测试是否可以正常连接hdfs进行增删查改
public class HadoopClusterTest {
    //程序的入口，JVM 会从此处开始执行
    public static void main(String[] args) {
        // 设置HADOOP_OPTS，跳过本地库加载（其实本地库已经配置了）
        System.setProperty("HADOOP_OPTS", "-Djava.library.path=");
        // 加载配置文件
        Configuration conf = new Configuration();
        conf.addResource(new Path("src/main/resources/hadoop/core-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/hdfs-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/mapred-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/yarn-site.xml"));
        // 连接HDFS
        //try (FileSystem fs = ...) 是 Java 的 try-with-resources 语法，
        //会自动关闭 fs 资源（无需手动调用 fs.close()）
        //配置对象 conf 获取 HDFS 文件系统实例（FileSystem）
        try (FileSystem fs = FileSystem.get(conf)) {
            //测试目录创建
            //定义 HDFS 目录路径（根目录下的 test_dir）
            Path testDir = new Path("/test_dir");
            //检查目录是否存在（exists 方法返回 boolean值）
            if (!fs.exists(testDir)) {
                //创建目录（mkdirs 可递归创建父目录，如 /a/b/c 不存在时会自动创建 a 和 b）
                boolean created = fs.mkdirs(testDir);
                //创建了created取1，没有创建则取0
                System.out.println("创建目录 " + testDir + " " + (created ? "成功" : "失败"));
            } else {
                System.out.println("目录已存在: " + testDir);
            }

            // 2. 测试文件创建与写入
            Path testFile = new Path(testDir, "test_file.txt");
            //testDir是路径，testFile是文件
            //编写content内容
            String content = "Hello, HDFS! This is a test file.";
            //同上，自动关闭 fs 资源
            //FSDataOutputStream out 意味着文件输出流，用于写入数据到 HDFS
            try (FSDataOutputStream out = fs.create(testFile, true)) { // true表示追加模式
                out.write(content.getBytes(StandardCharsets.UTF_8));//将字符串转换为 UTF-8 字节数组并写入文件
                System.out.println("文件写入成功: " + testFile);// 3. 测试文件读取 输出testFile的内容是Hello, HDFS! This is a test file.
            }

            // 3. 测试文件读取
            if (fs.exists(testFile)) {
                try (FSDataInputStream in = fs.open(testFile)) {
                    byte[] buffer = new byte[(int) fs.getFileStatus(testFile).getLen()];
                    in.readFully(buffer);
                    String readContent = new String(buffer, StandardCharsets.UTF_8);
                    System.out.println("文件内容:\n" + readContent);
                }
            }

            // 4. 测试文件删除
            boolean deleted = fs.delete(testFile, false); // false表示不递归删除
            System.out.println("删除文件 " + testFile + " " + (deleted ? "成功" : "失败"));

            // 5. 测试目录删除（递归）
            deleted = fs.delete(testDir, true); // true表示递归删除
            System.out.println("删除目录 " + testDir + " " + (deleted ? "成功" : "失败"));

        } catch (IOException e) {
            System.out.println("HDFS操作异常: " + e.getMessage());
            // 打印完整堆栈跟踪，便于定位具体错误
            e.printStackTrace();
        }
    }
}