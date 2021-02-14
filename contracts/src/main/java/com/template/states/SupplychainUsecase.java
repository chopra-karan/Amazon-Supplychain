package com.template.states;

import com.template.contracts.SupplychainUsecaseContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(SupplychainUsecaseContract.class)
public class SupplychainUsecase implements LinearState {
    private String cropName;
    private Integer quantity;
    private String farmerName;
    private String farmerState;
    private String wholesellarName;
    private String wholesellarState;
    private Integer amazonListedPrice;
    private String listedDate;
    private Party Farmer;
//    private Party WholeSellar;
    private Party AmazonAdmin;
    private final UniqueIdentifier linearId;

    public SupplychainUsecase(String cropName, Integer quantity, String farmerName, String farmerState, String wholesellarName, String wholesellarState, Integer amazonListedPrice, String listedDate, Party farmer,  Party amazonAdmin, UniqueIdentifier linearId) {
        this.cropName = cropName;
        this.quantity = quantity;
        this.farmerName = farmerName;
        this.farmerState = farmerState;
        this.wholesellarName = wholesellarName;
        this.wholesellarState = wholesellarState;
        this.amazonListedPrice = amazonListedPrice;
        this.listedDate = listedDate;
        Farmer = farmer;
//        WholeSellar = wholeSellar;
        AmazonAdmin = amazonAdmin;
        this.linearId = linearId;
    }

    public String getCropName() {
        return cropName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getFarmerState() {
        return farmerState;
    }

    public String getWholesellarName() {
        return wholesellarName;
    }

    public String getWholesellarState() {
        return wholesellarState;
    }

    public Integer getAmazonListedPrice() {
        return amazonListedPrice;
    }

    public String getListedDate() {
        return listedDate;
    }

    public Party getFarmer() {
        return Farmer;
    }

//    public Party getWholeSellar() {
//        return WholeSellar;
//    }

    public Party getAmazonAdmin() {
        return AmazonAdmin;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(Farmer, AmazonAdmin);
    }

    @Override
    public String toString() {
        return "SupplychainUsecase{" +
                "cropName='" + cropName + '\'' +
                ", quantity=" + quantity +
                ", farmerName='" + farmerName + '\'' +
                ", farmerState='" + farmerState + '\'' +
                ", wholesellarName='" + wholesellarName + '\'' +
                ", wholesellarState='" + wholesellarState + '\'' +
                ", amazonListedPrice=" + amazonListedPrice +
                ", listedDate='" + listedDate + '\'' +
                '}';
    }
}
