package Server;

import Handler.WebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Fisherman
 * @date 2020/6/6 23:08
 */
public class WebSocketServer {
    // 测试 main 方法 如果 main 方法不带有任何参数，那么就使用默认的 8000 端口
    public static void main(String[] args) throws InterruptedException {
        int port = 8000;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {

            }
        }
        new WebSocketServer().bind(port);
    }

    /**
     * <pre>服务端绑定逻辑</pre>
     *
     * @param port 端口号
     * @throws InterruptedException
     */
    public void bind(int port) throws InterruptedException {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //用于处理 HTTP 消息的编码和解码器:仅为协议升级请求服务一次（除非协议升级失败）
                                    .addLast("Http Codec",new HttpServerCodec())
                                    //用于支持 chunked 类型的 HTTP 情况，数字为支持的最大正文字节长度（超过就抛弃）,其也是仅在第一次协议升级后会被删除，所以这个 Handler 可以不加
                                    .addLast("Http Aggregator",new HttpObjectAggregator(65535))
                                    //用户支持服务器传输比较大的文件数据
                                    .addLast("Http Chunked",new ChunkedWriteHandler())
                                    //这是我们自己写的 WebSocket 处理器。
                                    .addLast("Socket Server",new WebSocketServerHandler());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
