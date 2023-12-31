package com.segc.services;

import com.segc.exception.DataIntegrityException;
import com.segc.transaction.SignedTransaction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
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
        byte[] previousDigest;
        long expectedBlockId;
        if (signedBlocks.isEmpty()) {
            previousDigest = new byte[32];
            expectedBlockId = 1;
        } else {
            SignedObject lastSignedBlock = signedBlocks.get(signedBlocks.size() - 1);
            Block lastBlock = (Block) lastSignedBlock.getObject();
            previousDigest = dps.getDigest(signedBlocks.get(signedBlocks.size() - 1));
            expectedBlockId = lastBlock.blockId + 1;
        }

        String suffix = SUFFIX_FORMATTER.format(expectedBlockId); // bloco parcial tem o id n+1
        Path filePath = Path.of(blockchainDir, PREFIX + suffix + PART_EXTENSION);
        try {
            this.block = dps.getObjects(Block.class, blockchainDir, f -> f.getName().endsWith(PART_EXTENSION)).get(0);
            if (this.block.blockId != expectedBlockId) {
                String message = String.format("Expected block id %d, got %d.", expectedBlockId, this.block.blockId);
                throw new DataIntegrityException(message);
            }
        } catch (IndexOutOfBoundsException | FileNotFoundException e) {
            this.block = new Block(previousDigest, expectedBlockId, new LinkedList<>());
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

            byte[] actualDigest = dps.getDigest(prev);
            Block prevBlock = (Block) Objects.requireNonNull(prev).getObject();
            if (prevBlock.blockId != expectedBlockId) {
                String message = String.format("Expected block id %d, got %d.", expectedBlockId, prevBlock.blockId);
                throw new DataIntegrityException(message);
            }

            byte[] expectedDigest = nextBlock.previousDigest;
            if (!Arrays.equals(actualDigest, expectedDigest)) {
                String message = String.format("At block %d: expected digest %s for previous block %d, got %s.",
                        nextBlock.blockId,
                        digestToHex(expectedDigest),
                        prevBlock.blockId,
                        digestToHex(actualDigest));
                throw new DataIntegrityException(message);
            }
            prev = next;
            expectedBlockId++;
        } while (next != null);
    }

    public void addTransaction(SignedTransaction t) {
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
            block = new Block(dps.getDigest(signedBlock), block.blockId + 1, new LinkedList<>());
            block.addTransaction(t);
        }
        String suffix = SUFFIX_FORMATTER.format(block.blockId);
        Path filePath = Path.of(blockchainDir, PREFIX + suffix + PART_EXTENSION);
        dps.putObject(block, filePath);
    }

    public LinkedList<SignedTransaction> getTransactions() {
        LinkedList<SignedTransaction> transactions = new LinkedList<>();
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

    /**
     * Converts a digest in byte array format into a hex-formatted string.
     *
     * @param digest the digest to format
     * @return a hex-formatted string
     * @see <a href="https://stackoverflow.com/a/943963">Stack Overflow Answer</a>
     */
    public static String digestToHex(byte[] digest) {
        BigInteger bi = new BigInteger(1, digest);
        return String.format("%0" + (digest.length << 1) + "X", bi);
    }

    private static class Block implements Serializable {
        private static final long serialVersionUID = 8425212697641963447L;
        private final byte[] previousDigest;
        private final long blockId;
        private long numTransactions;
        private final List<SignedTransaction> transactions;

        @Override
        public String toString() {
            return String.format("Block %s has %d transactions and points to hashed block %s.%n",
                    blockId,
                    numTransactions,
                    digestToHex(previousDigest));
        }

        public Block(byte[] previousDigest, long blockId, List<SignedTransaction> transactions) {
            this.previousDigest = previousDigest;
            this.blockId = blockId;
            this.transactions = transactions;
            this.numTransactions = transactions.size();
        }

        private void addTransaction(SignedTransaction t) {
            this.transactions.add(t);
            numTransactions++;
        }
    }
}
