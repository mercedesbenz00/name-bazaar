pragma solidity ^0.4.11;

import "SafeMath.sol";
import "OfferingLibrary.sol";

library EnglishAuctionOfferingLibrary {
    using SafeMath for uint;
    using OfferingLibrary for OfferingLibrary.Offering;

    struct EnglishAuctionOffering {
        uint  endTime;
        uint  extensionDuration;
        uint  minBidIncrease;
        address winningBidder;
    }

    event onBid(address indexed bidder, uint amount, uint endTime, uint datetime);

    function construct(
        EnglishAuctionOffering storage self,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(_endTime > now);
        self.endTime = _endTime;
        self.extensionDuration = _extensionDuration;
        require(_minBidIncrease > 0);
        self.minBidIncrease = _minBidIncrease;
    }

    function bid(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(now < self.endTime);

        if (self.winningBidder == 0x0) {
            require(msg.value >= offering.price);
        } else {
            require(msg.value >= offering.price.add(self.minBidIncrease));
            self.winningBidder.transfer(offering.price);
        }

        self.winningBidder = msg.sender;
        offering.price = msg.value;

        if ((self.endTime - self.extensionDuration) <= now) {
            self.endTime = now.add(self.extensionDuration);
        }

        onBid(msg.sender, offering.price, self.endTime, now);
        offering.setChanged();
    }

    function finalize(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(now > self.endTime);
        require(self.winningBidder != 0x0);
        offering.finalize(self.winningBidder, offering.price);
    }

    function reclaim(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        if (offering.isSenderEmergencyMultisig()) {
            if (!hasNoBids(self) && !offering.isEmergencyDisabled()) {
                self.winningBidder.transfer(offering.price);
            }
        } else {
            require(hasNoBids(self));
        }
        offering.reclaim();
    }

    function setSettings(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(offering.isSenderOriginalOwner());
        require(hasNoBids(self));
        offering.price = _startPrice;

        construct(
            self,
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
        offering.setChanged();
    }

    function hasNoBids(EnglishAuctionOffering storage self) returns(bool) {
        return self.winningBidder == 0x0;
    }


}

