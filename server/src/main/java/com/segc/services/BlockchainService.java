package com.segc.services;

import com.segc.exception.DataIntegrityException;
import com.segc.transaction.Transaction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.SignedObject;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class BlockchainService {

    /**
     * The filename prefix for a block.
     */
    public static final String PREFIX = "block_";
    public static final String EXTENSION = ".blk";
    public static final String PART_EXTENSION = ".part";

    /**
     * Left-pads the block ID in the block filename.
     */
    public static final DecimalFormat SUFFIX_FORMATTER = new DecimalFormat("0000000000");

    public static final long MAX_TRANSACTIONS_PER_BLOCK = 5;
    private final CipherService cipherService;
    private final DataPersistenceService dps;
    private final String blockchainDir;
    private List<SignedObject> signedBlocks;
    private Block block;

    public BlockchainService(String blockchainDir,
                             CipherService cipherService,
                             DataPersistenceService dps) {
        this.cipherService = cipherService;
        this.dps = dps;
        this.blockchainDir = blockchainDir;
        try {
            initBlockchain();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initBlockchain() throws
            DataIntegrityException,
            SignatureException,
            InvalidKeyException,
            IOException,
            ClassNotFoundException {
        try {
            signedBlocks = dps.getObjects(SignedObject.class, blockchainDir, f -> f.getName().endsWith(EXTENSION));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        String previousDigest;
        long blockId;
        if (signedBlocks.isEmpty()) {
            previousDigest = String.format("%08d", 0);
            blockId = 1;
        } else {
            SignedObject lastSignedBlock = signedBlocks.get(signedBlocks.size() - 1);
            Block lastBlock = (Block) lastSignedBlock.getObject();
            previousDigest = dps.getDigestAsHexString(signedBlocks.get(signedBlocks.size() - 1));
            blockId = lastBlock.blockId + 1;
        }

        String suffix = SUFFIX_FORMATTER.format(blockId); // bloco parcial tem o id n+1
        Path filePath = Path.of(blockchainDir, PREFIX + suffix + PART_EXTENSION);
        try {
            this.block = dps.getObjects(Block.class, blockchainDir, f -> f.getName().endsWith(PART_EXTENSION)).get(0);
        } catch (IndexOutOfBoundsException | FileNotFoundException e) {
            this.block = new Block(previousDigest, blockId, new LinkedList<>());
        }
        verifyBlockchain();
        dps.putObject(this.block, filePath);
    }

    private void verifyBlockchain() throws
            DataIntegrityException,
            SignatureException,
            InvalidKeyException,
            IOException,
            ClassNotFoundException {
        if (signedBlocks.size() == 0) {
            return;
        }

        Iterator<SignedObject> iter = signedBlocks.iterator();
        SignedObject prev = iter.next();
        if (!cipherService.verify(prev)) {
            throw new DataIntegrityException("The integrity of block " + block.blockId + " is compromised.");
        }

        SignedObject next;
        Block nextBlock;
        long expectedBlockId = 1L;
        do {
            if (iter.hasNext()) {
                next = iter.next();
                if (!cipherService.verify(next)) {
                    throw new DataIntegrityException("The integrity of block " + block.blockId + " is compromised.");
                }
                nextBlock = (Block) next.getObject();
            } else {
                next = null;
                nextBlock = this.block; // verificamos se o bloco parcial aponta para o último bloco assinado
            }

            String actualDigest = dps.getDigestAsHexString(prev);
            Block prevBlock = (Block) Objects.requireNonNull(prev).getObject();
            if (prevBlock.blockId != expectedBlockId) {
                String message = String.format("Expected block id %d, got %d.", expectedBlockId, prevBlock.blockId);
                throw new DataIntegrityException(message);
            }

            String expectedDigest = nextBlock.previousDigest;
            if (!Objects.equals(actualDigest, expectedDigest)) {
                String message = String.format("At block %d: expected digest %s for previous block %d, got %s.",
                        nextBlock.blockId,
                        expectedDigest,
                        prevBlock.blockId,
                        actualDigest);
                throw new DataIntegrityException(message);
            }
            prev = next;
            expectedBlockId++;
        } while (next != null);
    }

    public void addTransaction(Transaction t) {
        if (block.transactions.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            block.addTransaction(t);
        } else {
            SignedObject signedBlock = cipherService.sign(block);
            String suffix = SUFFIX_FORMATTER.format(block.blockId);

            String blockName = PREFIX + suffix;
            Path oldFilePath = Path.of(blockchainDir, blockName + PART_EXTENSION);
            Path newFilePath = Path.of(blockchainDir, blockName + EXTENSION);

            if (!dps.putObject(signedBlock, oldFilePath, newFilePath)) {
                throw new DataIntegrityException("Could not rename partial block file");
            }
            signedBlocks.add(signedBlock);
            block = new Block(dps.getDigestAsHexString(signedBlock), block.blockId + 1, new LinkedList<>());
            block.addTransaction(t);
        }
        String suffix = SUFFIX_FORMATTER.format(block.blockId);
        Path filePath = Path.of(blockchainDir, PREFIX + suffix + PART_EXTENSION);
        dps.putObject(block, filePath);
    }

    public LinkedList<Transaction> getTransactions() {
        LinkedList<Transaction> transactions = new LinkedList<>();
        for (SignedObject signedBlock : signedBlocks) {
            try {
                Block block = (Block) signedBlock.getObject();
                transactions.addAll(block.transactions);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        transactions.addAll(block.transactions);
        return transactions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder((signedBlocks.size() + 1) * 128);
        try {
            for (SignedObject signedBlock : signedBlocks) {
                sb.append(signedBlock.getObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return sb.append(block).toString();
    }

    private static class Block implements Serializable {
        private static final long serialVersionUID = 8425212697641963447L;
        private final String previousDigest;
        private final long blockId;
        private long numTransactions;
        private final List<Transaction> transactions;

        @Override
        public String toString() {
            return String.format("Block %s has %d transactions and points to hashed block %s.%n",
                    blockId,
                    numTransactions,
                    previousDigest);
        }

        public Block(String previousDigest, long blockId, List<Transaction> transactions) {
            this.previousDigest = previousDigest;
            this.blockId = blockId;
            this.transactions = transactions;
            this.numTransactions = transactions.size();
        }

        private void addTransaction(Transaction t) {
            this.transactions.add(t);
            numTransactions++;
        }
    }
}
