package main.java;

import java.util.*;

public class TxHandler {

    private UTXOPool ledger;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS

        this.ledger = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> utxoSet = new HashSet<>();
        double sumOfInputVals = 0, sumOfOutputVals = 0;
        int i = 0;
        for (Transaction.Input input: tx.getInputs()) {
            UTXO lastUTXO = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output prevTx = ledger.getTxOutput(lastUTXO);
            // check 1 - all output claimed by tx are in current utxopool
            if (!ledger.contains(lastUTXO)) {
                return false;
            }
            // check 2 - signatures of each input are valid
            if (input.signature == null || !Crypto.verifySignature(prevTx.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }
            utxoSet.add(lastUTXO);
            sumOfInputVals += prevTx.value;
            i++;
        }

        // check 3 - no utxo is claimed multiple times
        if (utxoSet.size() != tx.getInputs().size())
            return false;

        // check 4 - non negative output values
        for (Transaction.Output output: tx.getOutputs()) {
            sumOfOutputVals += output.value;
            if (output.value < 0)
                return false;
        }

        // check 5 - validating input values >= sum of output values
        if (sumOfInputVals < sumOfOutputVals)
            return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        //TODO: Still need to update UTXO pool "as necessary"
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
//		System.out.println("Size: " + possibleTxs.length);
        for(Transaction tx : possibleTxs){
            if (isValidTx(tx)) {
                //add tx to list of accepted transactions
                acceptedTxs.add(tx);

                //remove claimed UTXOs from the public ledger
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO claimedOutput = new UTXO(input.prevTxHash, input.outputIndex);
                    ledger.removeUTXO(claimedOutput);
                }

                //add new UTXOs to the public ledger
                byte[] txHash = tx.getHash();
                ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
                for(int i = 0; i < txOutputs.size(); i++) {
                    UTXO utxo = new UTXO(txHash, i);
                    ledger.addUTXO(utxo, txOutputs.get(i));
                }
            }
        }
        return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }


}
