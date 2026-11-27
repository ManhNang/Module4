package com.pokemon.blindbox.controller.web;

import com.pokemon.blindbox.dto.response.MasterSetProgressDTO;
import com.pokemon.blindbox.dto.response.WalletResponseDTO;
import com.pokemon.blindbox.dto.response.WalletTransactionResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.DigitalBinderService;
import com.pokemon.blindbox.service.SanctuaryService;
import com.pokemon.blindbox.service.SealedStoreVaultService;
import com.pokemon.blindbox.service.StoreQueryService;
import com.pokemon.blindbox.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WebViewController gọn nhẹ sau khi tái cấu trúc (P1-07):
 * - Chỉ phụ thuộc vào tầng Service (không inject bất kỳ Repository nào).
 * - Không mở @Transactional ghi DB trong các trang HTTP GET.
 * - Không truyền Entity trực tiếp ra Thymeleaf View (P3-04).
 */
@Controller
@RequiredArgsConstructor
public class WebViewController {

    private final StoreQueryService storeQueryService;
    private final DigitalBinderService digitalBinderService;
    private final SanctuaryService sanctuaryService;
    private final SealedStoreVaultService sealedStoreVaultService;
    private final WalletService walletService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping("/")
    public String homeRedirect() {
        return "redirect:/store";
    }

    /**
     * 1. Trang Cửa Hàng Đa Bộ Sưu Tập (Pack 6 thẻ + Box nguyên seal 20 Pack -10% +
     * Daily Global Stock)
     */
    @GetMapping({ "/store", "/shop/boxes" })
    public String shopBoxTrayPage(
            @RequestParam(value = "boxId", required = false) Long boxId,
            Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.StorePageData data = storeQueryService.loadStorePage(boxId);
        List<SealedStoreVaultService.StoreSetCatalogItem> storeSets = sealedStoreVaultService
                .getStoreCatalogWithDailyStock();

        populateCommonUserAndWallet(model, "store", userId);
        model.addAttribute("storeSets", storeSets);
        model.addAttribute("rarities", data.rarities());
        model.addAttribute("totalDropWeight", data.totalDropWeight());

        return "shop/box-tray";
    }

    /**
     * 2. Trang Kho Đồ (Inventory & Master Set 103 ô)
     */
    @GetMapping({ "/inventory", "/binder" })
    public String inventoryAndBinderPage(
            @RequestParam(value = "setId", required = false) Long setId,
            @RequestParam(value = "tab", defaultValue = "inventory") String initialTab,
            Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.InventoryPageData data = storeQueryService.loadInventoryPage(userId);
        MasterSetProgressDTO progress;
        if (data.cardSets().isEmpty()) {
            progress = MasterSetProgressDTO.builder()
                    .userId(userId)
                    .setId(0L)
                    .setName("Chưa có bộ sưu tập")
                    .setCode("---")
                    .baseSetTotal(0)
                    .ownedBaseCardCount(0)
                    .formattedBaseProgress("0 / 0")
                    .baseCompletionPercentage(java.math.BigDecimal.ZERO)
                    .totalSecretRareInSet(0)
                    .ownedSecretRareCount(0)
                    .baseSetSlots(List.of())
                    .secretRareSlots(List.of())
                    .build();
        } else {
            Long resolvedSetId = (setId != null && data.cardSets().stream().anyMatch(s -> s.id().equals(setId)))
                    ? setId
                    : data.cardSets().getFirst().id();
            progress = digitalBinderService.getMasterSetProgress(userId, resolvedSetId);
        }

        Set<Long> expeditionCardIds = sanctuaryService.getActiveExpeditionCardIds(userId);
        List<SealedStoreVaultService.SealedSetVaultGroup> sealedGroups = sealedStoreVaultService
                .getUserSealedVaultGroups(userId);
        long totalSealedCount = sealedGroups.stream()
                .mapToLong(g -> g.availablePacks() + g.lockedPacks() + g.availableBoxes() + g.lockedBoxes())
                .sum();

        populateCommonUserAndWallet(model, "inventory", userId);
        model.addAttribute("initialTab", initialTab);
        model.addAttribute("cardSets", data.cardSets());
        model.addAttribute("rarities", data.rarities());
        model.addAttribute("speciesList", data.speciesList());
        model.addAttribute("myCards", data.myCards());
        model.addAttribute("expeditionCardIds", expeditionCardIds);
        model.addAttribute("sealedGroups", sealedGroups);
        model.addAttribute("totalSealedCount", totalSealedCount);
        model.addAttribute("templateCopyCounts", data.templateCopyCounts());
        model.addAttribute("templateSellableCounts", data.templateSellableCounts());
        model.addAttribute("totalCardCopies", data.totalCardCopies());
        model.addAttribute("unlockedCardCount", data.unlockedCardCount());
        model.addAttribute("listedCardCount", data.listedCardCount());
        model.addAttribute("progress", progress);

        return "binder/index";
    }

    /**
     * 2B. Trang Chi Tiết Thẻ Bài theo ID
     */
    @GetMapping("/cards/{cardId}")
    public String cardDetailPage(@PathVariable Long cardId, Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.CardDetailPageData data = storeQueryService.loadCardDetailById(userId, cardId);
        return populateCardDetailModel(model, userId, data);
    }

    /**
     * 2C. Trang Chi Tiết Thẻ Bài theo Số thứ tự
     */
    @GetMapping("/cards/number/{cardNumber}")
    public String cardDetailByNumberPage(
            @PathVariable Integer cardNumber,
            @RequestParam(value = "setId", defaultValue = "1") Long setId,
            Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.CardDetailPageData data = storeQueryService.loadCardDetailByNumber(userId, setId, cardNumber);
        return populateCardDetailModel(model, userId, data);
    }

    private String populateCardDetailModel(Model model, Long userId, StoreQueryService.CardDetailPageData data) {
        populateCommonUserAndWallet(model, "inventory", userId);
        model.addAttribute("card", data.card());
        model.addAttribute("isHoloRarity", data.isHoloRarity());
        model.addAttribute("ownedCopiesCount", data.ownedCopiesCount());
        model.addAttribute("firstUnlockedUserCardId", data.firstUnlockedUserCardId());
        model.addAttribute("setCards", data.setCards());
        model.addAttribute("prevCard", data.prevCard());
        model.addAttribute("nextCard", data.nextCard());
        model.addAttribute("dropRatePercent", data.dropRatePercent());
        return "cards/detail";
    }

    /**
     * 3. Trang Sàn Giao Dịch P2P & Đấu Giá
     */
    @GetMapping("/market")
    public String marketplacePage(
            @RequestParam(value = "setName", required = false) String setName,
            @RequestParam(value = "speciesName", required = false) String speciesName,
            @RequestParam(value = "sellUserCardId", required = false) Long sellUserCardId,
            @RequestParam(value = "sellSealedId", required = false) Long sellSealedId,
            @RequestParam(value = "sellItemType", required = false) String sellItemType,
            Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.MarketPageData data = storeQueryService.loadMarketPage(userId, setName, speciesName);
        List<SealedStoreVaultService.SealedMarketOption> sellableSealed = sealedStoreVaultService
                .getSellableSealedOptions(userId);

        populateCommonUserAndWallet(model, "market", userId);
        model.addAttribute("fixedPriceListings", data.fixedPriceListings());
        model.addAttribute("activeAuctions", data.activeAuctions());
        model.addAttribute("sellerNameMap", data.sellerNameMap());
        model.addAttribute("bidCountMap", data.bidCountMap());
        model.addAttribute("myCards", data.myCards());
        model.addAttribute("myCardCopyCounts", data.myCardCopyCounts());
        model.addAttribute("activeAuctionCount", data.activeCount());
        model.addAttribute("mineAuctionCount", data.mineCount());
        model.addAttribute("endedAuctionCount", data.endedCount());
        model.addAttribute("sellableSealed", sellableSealed);
        model.addAttribute("cardSets", data.cardSets());
        model.addAttribute("rarities", data.rarities());
        model.addAttribute("speciesList", data.speciesList());
        model.addAttribute("activeCrossFilter", data.activeCrossFilter());
        model.addAttribute("selectedSetName", data.selectedSetName());
        model.addAttribute("selectedSpeciesName", data.selectedSpeciesName());
        model.addAttribute("sellUserCardId", sellUserCardId);
        model.addAttribute("sellSealedId", sellSealedId);
        model.addAttribute("sellItemType", sellItemType);

        return "market/index";
    }

    /**
     * 4. Trang Phòng Đấu Giá Trực Tiếp
     */
    @GetMapping("/market/auctions/{auctionId}")
    public String auctionDetailPage(@PathVariable Long auctionId, Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.AuctionDetailPageData data = storeQueryService.loadAuctionDetailPage(auctionId);

        populateCommonUserAndWallet(model, "market", userId);
        model.addAttribute("auction", data.auction());
        model.addAttribute("bidHistory", data.bidHistory());
        model.addAttribute("currentLeaderMasked", data.currentLeaderMasked());
        model.addAttribute("nextMinBid", data.nextMinBid());
        model.addAttribute("sellerDisplayName", data.sellerDisplayName());

        return "market/auction-detail";
    }

    /**
     * 5. Trang Admin Dashboard (ROLE_ADMIN) — hỗ trợ chuyển đổi đa Bộ sưu tập qua
     * ?setId=
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboardPage(
            @RequestParam(value = "setId", required = false) Long setId,
            Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        StoreQueryService.AdminDashboardPageData data = storeQueryService.loadAdminDashboardPage(setId);

        int nextCardNumber = data.cardTemplates().stream()
                .mapToInt(c -> c.cardNumber() != null ? c.cardNumber() : 0)
                .max()
                .orElse(0) + 1;

        populateCommonUserAndWallet(model, "admin", userId);
        model.addAttribute("cardSets", data.cardSets());
        model.addAttribute("activeSet", data.activeSet());
        model.addAttribute("todaySalesMap", sealedStoreVaultService.getTodaySalesMap());
        model.addAttribute("rarities", data.rarities());
        model.addAttribute("totalDropWeight", data.totalDropWeight());
        model.addAttribute("speciesList", data.speciesList());
        model.addAttribute("cardTemplates", data.cardTemplates());
        model.addAttribute("nextCardNumber", nextCardNumber);
        model.addAttribute("boosterBoxes", data.boosterBoxes());
        model.addAttribute("sealedAvailableBoxCount", data.sealedAvailableBoxCount());

        return "admin/dashboard";
    }

    /**
     * 5B. Trang Đảo Nuôi Dưỡng & Thám Hiểm 2.5D (Poke-Sanctuary Idle)
     * Nạp sẵn trạng thái Đảo ngay từ SSR để trang hiển thị tức thì không bị chớp
     * rỗng rồi ghi đè bằng AJAX.
     */
    @GetMapping("/sanctuary")
    public String sanctuaryPage(Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        Map<String, Object> initialSanctuaryState = sanctuaryService.getSanctuaryState(userId);
        populateCommonUserAndWallet(model, "sanctuary", userId);
        model.addAttribute("initialSanctuaryState", initialSanctuaryState);
        return "sanctuary/index";
    }

    /**
     * 6. Trang Ví Điện Tử & Nạp Tiền VNPay
     */
    @GetMapping("/wallet")
    public String walletPage(Model model) {
        Long userId = authUserProvider.getRequiredUserId();
        List<WalletTransactionResponseDTO> transactions = walletService.getRecentTransactionsByUserId(userId);

        populateCommonUserAndWallet(model, "wallet", userId);
        model.addAttribute("transactions", transactions);

        return "wallet/index";
    }

    private void populateCommonUserAndWallet(Model model, String activeTab, Long userId) {
        WalletResponseDTO wallet = walletService.getWalletByUserId(userId);
        String username = authUserProvider.getCurrentUsername();
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentUsername", username);
        model.addAttribute("wallet", wallet);
    }
}
