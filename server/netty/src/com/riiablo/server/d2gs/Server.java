package com.riiablo.server.d2gs;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntIntMap;

import com.riiablo.net.packet.netty.Connection;
import com.riiablo.net.packet.netty.Disconnect;
import com.riiablo.net.packet.netty.Netty;
import com.riiablo.net.packet.netty.NettyData;
import com.riiablo.nnet.Endpoint;
import com.riiablo.nnet.PacketProcessor;
import com.riiablo.nnet.tcp.TcpEndpoint;

public class Server implements PacketProcessor {
  private static final String TAG = "D2GS";

  static final int MAX_CLIENTS = Main.MAX_CLIENTS;

  private final InetAddress address;
  private final int port;
  private final D2GSPacketProcessor packetProcessor;

  private ChannelFuture future;
  private ServerBootstrap bootstrap;

  private Endpoint.IdResolver<?> idResolver;
  private Endpoint<?> endpoint;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  private final ConnectionLimiter connectionLimiter = new ConnectionLimiter(MAX_CLIENTS);
  private final ChannelInboundHandler connectionListener = new ChannelInboundHandlerAdapter() {
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);
      notifyChannelInactive(ctx);
    }
  };

  private final ClientData[] clients = new ClientData[MAX_CLIENTS]; {
    for (int i = 0; i < MAX_CLIENTS; i++) clients[i] = new ClientData();
    idResolver = new Endpoint.IdResolver<Channel>() {
      @Override
      public Channel get(int id) {
        return clients[id].channel;
      }

      @Override
      public String toString() {
        return ArrayUtils.toString(clients);
      }
    };
  }

  final IntIntMap player = new IntIntMap();

  public Server(InetAddress address, int port, D2GSPacketProcessor packetProcessor) {
    this.address = address;
    this.port = port;
    this.packetProcessor = packetProcessor;
  }

  public ChannelFuture future() {
    return future;
  }

  public Endpoint.IdResolver resolver() {
    return idResolver;
  }

  @SuppressWarnings("unchecked")
  private static Endpoint<?> createEndpoint(Endpoint.IdResolver<?> idResolver, PacketProcessor packetProcessor) {
    return new TcpEndpoint((Endpoint.IdResolver<Channel>) idResolver, packetProcessor);
  }

  @SuppressWarnings("unchecked")
  private static <T> EndpointedChannelHandler<T> createChannelHandler(Class<T> packetType, Endpoint<?> endpoint) {
    return new EndpointedChannelHandler<>(packetType, (Endpoint<T>) endpoint);
  }

  public void create() {
    Validate.validState(bossGroup == null);
    Validate.validState(workerGroup == null);
    Validate.validState(bootstrap == null);

    endpoint = createEndpoint(idResolver, this);
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    bootstrap = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            Gdx.app.debug(TAG, "initChannel " + ch);
            ch.pipeline()
                .addFirst(connectionLimiter)
                .addLast(connectionListener)
                .addLast(new SizePrefixedDecoder())
                .addLast(createChannelHandler(ByteBuf.class, endpoint))
                ;
          }
        })
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
//        .localAddress(address, port)
        ;
  }

  public ChannelFuture start() {
    Gdx.app.log(TAG, "Starting server...");
    Validate.validState(future == null);
    Gdx.app.debug(TAG, "attempting to bind to " + bootstrap.config().localAddress());
    future = bootstrap.bind(port);
    future.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Gdx.app.log(TAG, "successfully bound to " + future.channel().localAddress());
      }
    });
    return future;
  }

  public void update(float delta) {
  }

  public void dispose() {
    try {
      Gdx.app.debug(TAG, "shutting down channel...");
      future.channel().close();
      future.channel().closeFuture().syncUninterruptibly();
    } catch (Throwable t) {
      Gdx.app.error(TAG, t.getMessage(), t);
    }

    try {
      Gdx.app.debug(TAG, "shutting down workerGroup...");
      workerGroup.shutdownGracefully().syncUninterruptibly();
    } catch (Throwable t) {
      Gdx.app.error(TAG, t.getMessage(), t);
    }

    try {
      Gdx.app.debug(TAG, "shutting down bossGroup...");
      bossGroup.shutdownGracefully().syncUninterruptibly();
    } catch (Throwable t) {
      Gdx.app.error(TAG, t.getMessage(), t);
    }
  }

  @Override
  public void processPacket(ChannelHandlerContext ctx, SocketAddress from, ByteBuf bb) {
    Gdx.app.debug(TAG, "Processing packet...");
    Gdx.app.debug(TAG, "  " + ByteBufUtil.hexDump(bb));
    processPacket(ctx, from, Netty.getRootAsNetty(bb.nioBuffer()));
  }

  public void processPacket(ChannelHandlerContext ctx, SocketAddress from, Netty netty) {
    Gdx.app.debug(TAG, "  " + "dataType=" + NettyData.name(netty.dataType()));
    switch (netty.dataType()) {
      case NettyData.Connection:
        Connection(ctx, from, netty);
        break;
      case NettyData.Disconnect:
        Disconnect(ctx, from, netty);
        break;
      default:
        Gdx.app.debug(TAG, "  " + "not connection-related. propagating to " + packetProcessor);
        packetProcessor.processPacket(ctx, from, netty);
    }
  }

  private void Connection(ChannelHandlerContext ctx, SocketAddress from, Netty netty) {
    Gdx.app.debug(TAG, "Connection from " + from);
    Connection connection = (Connection) netty.data(new Connection());

    boolean generateSalt = true;
    long clientSalt = connection.salt();
    Gdx.app.debug(TAG, "  " + String.format("client salt=%016x", clientSalt));

    synchronized (clients) {
      final ClientData client;
      final ClientData[] clients = this.clients;

      int id;
      for (id = 0; id < MAX_CLIENTS && !from.equals(clients[id].address); id++);
      if (id == MAX_CLIENTS) {
        Gdx.app.debug(TAG, "  " + "no connection record found for " + from);
        Gdx.app.debug(TAG, "  " + "creating connection record for " + from);

        for (id = 0; id < MAX_CLIENTS && clients[id].connected; id++);
        assert id != MAX_CLIENTS : "no available client slots. connection limiter should have caught this";
        if (id == MAX_CLIENTS) {
          Gdx.app.error(TAG, "  " + "client connected, but no slot is available");
          Gdx.app.debug(TAG, "  " + "closing " + ctx);
          ctx.close();
          return;
        }

        Gdx.app.debug(TAG, "  " + "assigned " + from + " to " + id);
        client = clients[id].connect(ctx.channel(), from, clientSalt);
      } else {
        Gdx.app.debug(TAG, "  " + "found connection record for " + from + " as " + id);
        client = clients[id];
        Gdx.app.debug(TAG, "  " + "checking client salt");
        if (client.clientSalt == clientSalt) {
          Gdx.app.debug(TAG, "  " + "client salt matches server record");
          generateSalt = false;
        } else {
          Gdx.app.debug(TAG, "  " + "client salt mismatch with server record");
          Gdx.app.debug(TAG, "  " + "updating client salt to server record");
          clientSalt = client.clientSalt;
          Gdx.app.debug(TAG, "  " + String.format("client salt=%016x", clientSalt));
        }
      }

      long serverSalt;
      if (generateSalt) {
        Gdx.app.debug(TAG, "  " + "generating server salt");
        if (client.serverSalt != 0L) {
          Gdx.app.debug(TAG, "  " + String.format("overwriting existing server salt %016x", client.serverSalt));
        }
        serverSalt = client.serverSalt = MathUtils.random.nextLong();
        Gdx.app.debug(TAG, "  " + String.format("server salt=%016x", serverSalt));
      } else {
        serverSalt = client.serverSalt;
      }

      long salt = client.xor = clientSalt ^ serverSalt;
      Gdx.app.debug(TAG, "  " + String.format("salt=%016x", salt));
    }
  }

  private void Disconnect(ChannelHandlerContext ctx, SocketAddress from, Netty netty) {
    Gdx.app.debug(TAG, "Disconnect from " + from);
    Disconnect disconnect = (Disconnect) netty.data(new Disconnect());
    disconnect(ctx, from);
  }

  private void disconnect(ChannelHandlerContext ctx, SocketAddress from) {
    Gdx.app.debug(TAG, "  " + "disconnecting " + from);
    synchronized (clients) {
      int id;
      for (id = 0; id < MAX_CLIENTS && !from.equals(clients[id].address); id++) ;
      if (id == MAX_CLIENTS) {
        Gdx.app.debug(TAG, "  " + "client from " + from + " already disconnected");
      } else {
        Gdx.app.debug(TAG, "  " + "found connection record for " + from + " as " + id);
        Gdx.app.debug(TAG, "  " + "disconnecting " + id);
        clients[id].disconnect();
      }

      Gdx.app.debug(TAG, "  " + "closing " + ctx);
      ctx.close();
    }
  }

  private void notifyChannelInactive(ChannelHandlerContext ctx) {
    Gdx.app.debug(TAG, "notifyChannelInactive");
    SocketAddress from = endpoint.getSender(ctx, null);
    disconnect(ctx, from);
  }

  @ChannelHandler.Sharable
  private static class ConnectionLimiter extends ChannelInboundHandlerAdapter {
    static final String TAG = "ConnectionLimiter";

    final int maxClients;
    final AtomicInteger connections = new AtomicInteger();

    ConnectionLimiter(int maxClients) {
      this.maxClients = maxClients;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      int count = connections.incrementAndGet();
      if (count <= maxClients) {
        Gdx.app.debug(TAG, String.format("connection accepted. %d / %d", count, maxClients));
        super.channelActive(ctx);
      } else {
        Gdx.app.debug(TAG, "  " + "closing " + ctx);
        ctx.close();
        Gdx.app.debug(TAG, String.format("connection closed. maximum concurrent connections reached %d / %d", count, maxClients));
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);
      int count = connections.decrementAndGet();
      Gdx.app.debug(TAG, String.format("connection closed. %d / %d", count, maxClients));
    }
  }

  private static class ClientData {
    long clientSalt;
    long serverSalt;
    long xor;
    byte state;
    Channel channel;
    SocketAddress address;
    boolean connected;

    ClientData connect(Channel channel, SocketAddress address, long clientSalt) {
      assert !connected;
      this.channel = channel;
      this.address = address;
      this.clientSalt = clientSalt;
      connected = true;
      return this;
    }

    ClientData disconnect() {
      assert connected;
      connected = false;
      channel = null;
      address = null;
      return this;
    }

    @Override
    public String toString() {
      return connected ? String.format("[%016x: %s]", xor, address) : "[disconnected]";
    }
  }

  private static class SizePrefixedDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      if (in.readableBytes() < 4) return;
      in.markReaderIndex();
      final int length = in.readIntLE();
      if (in.readableBytes() < length) {
        in.resetReaderIndex();
        return;
      }
      out.add(in.readRetainedSlice(length));
    }
  }
}
