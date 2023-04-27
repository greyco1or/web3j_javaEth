package com.example.java_web3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;
import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@SpringBootTest
class JavaWeb3ApplicationTests {
    @Test
    public void helloWorld() {
        System.out.println("Hello World");
    }

    @Test
    public void connectInfura() {
        URI uri = null;
        try {
            uri = new URI("wss://mainnet.infura.io/ws/v3/246a412f52f74832b07b645d3c9b9fed");
        } catch (URISyntaxException e) {
            // 예외 처리 코드
            System.out.println("################### uri ERROR #######################");
        }

        //WebSocket 연결
        WebSocketClient webSocketClient = new WebSocketClient(uri);
        WebSocketService webSocketService = new WebSocketService(webSocketClient, false);
        try {
            webSocketService.connect();
        } catch (ConnectException e) {
            throw new RuntimeException(e);
        }

        // Web3j 설정
        Web3j web3j = Web3j.build(webSocketService);

        // RPC 요청
        Request<?, EthBlockNumber> request = web3j.ethBlockNumber();
        EthBlockNumber ethBlockNumber = null;
        try {
            ethBlockNumber = request.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BigInteger blockNumber = ethBlockNumber.getBlockNumber();
        EthBlock.Block block = null;
        try {
            block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
            //thBlock.BlockHeader header = block.getBlockHeader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Latest block number : " + blockNumber);
        System.out.println("Block : " + block);
        System.out.println("블록 번호: " + block.getNumber());
        System.out.println("블록 해시: " + block.getHash());
        System.out.println("블록 생성 시간: " + new Date(block.getTimestamp().longValue() * 1000));
        System.out.println("블록에 포함된 트랜잭션 수: " + block.getTransactions().size());
        List<EthBlock.TransactionResult> transactionResultList = block.getTransactions();
        for(EthBlock.TransactionResult transactionResult : transactionResultList) {
            EthBlock.TransactionResult<String> transactionHash = transactionResult;
            String txHash = transactionHash.get();
            System.out.println(txHash);
            Request<?, EthTransaction> transactionReq = web3j.ethGetTransactionByHash(txHash);
            Transaction transaction = null;
            try {
                transaction = transactionReq.send().getResult();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("트랜잭션 VALUE : " + transaction.getValue());
            System.out.println("트랜잭션 TO 주소 : " + transaction.getTo());
            System.out.println("트랜잭션 FROM 주소 : " + transaction.getFrom());
            System.out.println("트랜잭션 NONCE : " + transaction.getNonce());
            System.out.println("트랜잭션 INPUT DATA : " + transaction.getInput());
        }
        System.out.println("블록에 포함된 트랜잭션 : " + block.getTransactions());
        System.out.println("블록에 포함된 Uncle블록들 : " + block.getUncles());

        // WebSocket 연결 종료
        webSocketService.close();
    }

    @Test
    public static void main(String[] args) throws InterruptedException {
        URI uri = null;
        try {
            uri = new URI("wss://mainnet.infura.io/ws/v3/246a412f52f74832b07b645d3c9b9fed");
        } catch (URISyntaxException e) {
            System.out.println("URI syntax exception: " + e.getMessage());
            System.exit(0);
        }

        // create WebSocket client and service instances
        WebSocketClient webSocketClient = new WebSocketClient(uri);
        WebSocketService webSocketService = new WebSocketService(webSocketClient, true);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            webSocketService.connect();
        } catch (Exception e) {
            System.out.println("WebSocket connection failed: " + e.getMessage());
            System.exit(0);
        }

        // create Web3j instance
        Web3j web3j = Web3j.build(webSocketService, 2000, scheduledExecutorService);

        // listen for new blocks and print their information
        web3j.blockFlowable(false).forEach(block -> {
            System.out.println("Block number: " + block.getBlock().getNumber());
            System.out.println("Block hash: " + block.getBlock().getHash());
            System.out.println("Block timestamp: " + new Date(block.getBlock().getTimestamp().longValue() * 1000));
            System.out.println("Block transactions count: " + block.getBlock().getTransactions().size());
        });

        // keep the application running
        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

}
