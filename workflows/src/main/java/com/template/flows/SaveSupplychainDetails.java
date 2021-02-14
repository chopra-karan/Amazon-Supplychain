package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.SupplychainUsecaseContract;
import com.template.states.SupplychainUsecase;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@StartableByRPC
public class SaveSupplychainDetails extends FlowLogic<SignedTransaction> {
    private String cropName;
    private Integer quantity;
    private String farmerName;
    private String farmerState;
    private String wholesellarName;
    private String wholesellarState;
    private Integer amazonListedPrice;
    private String listedDate;
    private Party Farmer;
    private Party WholeSellar;
    private Party AmazonAdmin;
    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
    // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
    // function.
    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
    );

    public SaveSupplychainDetails(String cropName,
                                  Integer quantity,
                                  String farmerName,
                                  String farmerState,
                                  String wholesellarName,
                                  String wholesellarState,
                                  Integer amazonListedPrice,
                                  String listedDate) {
        this.cropName = cropName;
        this.quantity = quantity;
        this.farmerName = farmerName;
        this.farmerState = farmerState;
        this.wholesellarName = wholesellarName;
        this.wholesellarState = wholesellarState;
        this.amazonListedPrice = amazonListedPrice;
        this.listedDate = listedDate;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        // Generate an unsigned transaction.
        // me annonates the current owner, who is running this flow ie. Farmer in this case
        Farmer = getOurIdentity();
        // get wholesellar party
//        WholeSellar = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=WholeSellar,L=New York,C=US"));
        // get amazonAdmin Party
        AmazonAdmin = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse("O=AmazonAdmin,L=New York,C=US"));

        // save the value in state object
        SupplychainUsecase supplychainUsecase = new SupplychainUsecase(cropName, quantity, farmerName, farmerState, wholesellarName, wholesellarState, amazonListedPrice, listedDate, Farmer, AmazonAdmin, new UniqueIdentifier());

        // create the command
        final Command<SupplychainUsecaseContract.Commands.Create> txCommand = new Command<>(
                new SupplychainUsecaseContract.Commands.Create(),
                Arrays.asList(supplychainUsecase.getFarmer().getOwningKey(), supplychainUsecase.getAmazonAdmin().getOwningKey()));

        //build the transaction
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(supplychainUsecase, SupplychainUsecaseContract.ID)
                .addCommand(txCommand);

        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        // Verify that the transaction is valid.
        txBuilder.verify(getServiceHub());

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        // Sign the transaction.
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession otherPartySession = initiateFlow(AmazonAdmin);
//        FlowSession otherPartySession1 = initiateFlow(WholeSellar);
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
    }


    @InitiatedBy(SaveSupplychainDetails.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction.", output instanceof SupplychainUsecase);
//                        IOUState iou = (IOUState) output;
//                        require.using("I won't accept IOUs with a value over 100.", iou.getValue() <= 100);
                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }
}


