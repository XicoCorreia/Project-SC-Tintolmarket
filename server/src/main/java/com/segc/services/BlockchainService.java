package com.segc.services;

import com.segc.exception.DataIntegrityException;
import com.segc.services.CipherService;
import com.segc.services.DataPersistenceService;
import com.segc.transaction.Transaction;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.SignedObject;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

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

    /**
     * Left-pads the block ID in the block filename.
     */
    public static final DecimalFormat SUFFIX_FORMATTER = new DecimalFormat("0000000000");

    public static final long MAX_TRANSACTIONS_PER_BLOCK = 5;
    private final CipherService cipherService;
    private final DataPersistenceService dps;
    private final String blockchainDir;
    private final List<SignedObject> signedBlocks;
    private Block block;

    public BlockchainService(String blockchainDir,
                             CipherService cipherService,
                             DataPersistenceService dps) {
        this.cipherService = cipherService;
        this.dps = dps;
        this.blockchainDir = blockchainDir;
        this.signedBlocks = new LinkedList<>();
        try {
            initBlockchain();
        } catch (SignatureException | InvalidKeyException | DataIntegrityException e) {
            throw new RuntimeException(e);
        }
    }

    private void initBlockchain() throws DataIntegrityException, SignatureException, InvalidKeyException {
        for (SignedObject signedBlock : dps.getObjects(SignedObject.class, blockchainDir)) {
            if (cipherService.verify(signedBlock)) {
                signedBlocks.add(signedBlock);
            } else {
                throw new DataIntegrityException("The integrity of block " + block.blockId + " is compromised.");
            }
        }
        if (!signedBlocks.isEmpty()) {
            this.block = dps.getObjects(Block.class, Path.of(blockchainDir, "part").toString()).get(0);
        } else {
            this.block = new Block(String.format("%08d", 0), 1, new LinkedList<>());
            String suffix = SUFFIX_FORMATTER.format(block.blockId);
            Path filePath = Path.of(blockchainDir, "part", PREFIX + suffix + EXTENSION);
            dps.putObject(this.block, filePath);
        }
    }

    public void addTransaction(Transaction t) {
        if (block.transactions.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            block.addTransaction(t);
        } else {
            SignedObject signedBlock = cipherService.sign(block);
            String suffix = SUFFIX_FORMATTER.format(block.blockId);
            Path filePath = Path.of(blockchainDir, "part", PREFIX + suffix + EXTENSION);
            Path newFilePath = Path.of(blockchainDir, PREFIX + suffix + EXTENSION);
            if (!dps.putObject(signedBlock, filePath, newFilePath)) {
                throw new DataIntegrityException("Could not rename partial block file");
            }
            signedBlocks.add(signedBlock);
            block = new Block(dps.getDigestAsHexString(signedBlock), block.blockId + 1, new LinkedList<>());
            block.addTransaction(t);
        }
        String suffix = SUFFIX_FORMATTER.format(block.blockId);
        Path filePath = Path.of(blockchainDir, "part", PREFIX + suffix + EXTENSION);
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
