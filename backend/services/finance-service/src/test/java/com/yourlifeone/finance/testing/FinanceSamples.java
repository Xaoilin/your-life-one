package com.yourlifeone.finance.testing;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Request bodies with obviously fake data ("Example Bank", "Example Co", round pence amounts). Never put
 * real financial data in tests or fixtures.
 */
public final class FinanceSamples {
    public static final JsonMapper JSON = JsonMapper.builder().build();

    private FinanceSamples() {
    }

    public static String account(String name, long balance, int sortOrder) {
        return """
                {"name":"%s","type":"current","balance":%d,"currency":"GBP","color":"#22c55e","icon":"bank",
                 "includeInNetWorth":true,"sortOrder":%d}""".formatted(name, balance, sortOrder);
    }

    public static String income(String accountId, long amount, String date) {
        return transaction("income", amount, "salary", accountId, null, date);
    }

    public static String expense(String accountId, long amount, String date) {
        return transaction("expense", amount, "groceries", accountId, null, date);
    }

    public static String transfer(String from, String to, long amount, String date) {
        return transaction("transfer", amount, "transfer", from, to, date);
    }

    public static String transaction(String type, long amount, String category, String accountId, String toAccountId,
            String date) {
        var to = toAccountId == null ? "null" : "\"" + toAccountId + "\"";
        return """
                {"type":"%s","amount":%d,"category":"%s","accountId":"%s","toAccountId":%s,
                 "description":"Example payment","date":"%s","tags":["example"]}"""
                .formatted(type, amount, category, accountId, to, date);
    }

    /** A complete banking review; its first loan knows nothing optional, its second everything. */
    public static ObjectNode reviewDraft() {
        return (ObjectNode) JSON.readTree("""
                {"asOf":"2026-09-20","currency":"GBP",
                 "coverage":{"from":"2026-06-01","to":"2026-08-31","completeThrough":"2026-08","transactionCount":120,
                             "accountCount":2,"note":"Example statements"},
                 "accounts":[{"id":"example-current","label":"Example Bank current","ownership":"personal",
                              "balancePence":12345,"asOf":"2026-09-20"}],
                 "months":[{"month":"2026-08","incomePence":250000,"outflowPence":200000,"netPence":50000,
                            "salaryPence":240000,"categories":[{"label":"Groceries","amountPence":-30000}]}],
                 "budget":{"incomePence":250000,"incomeBasis":"Example salary",
                           "essentials":[{"label":"Rent","amountPence":100000,"note":""}],
                           "workCostsPence":5000,"workCostsNote":"Travel",
                           "scenarios":[{"label":"Cheaper phone plan","status":"planned","monthlyAdjustmentPence":-1000,
                                         "note":""}]},
                 "opportunities":[{"label":"Streaming","monthlyPence":1500,"note":"Two services","suggestedCapPence":null}],
                 "loans":[{"id":"loan-1","lender":"Example Lender","purpose":"Car","status":"active",
                           "monthlyPaymentPence":null,"userSharePence":null,"balancePence":null,"balanceAsOf":null,
                           "balanceKind":"unknown","startDate":null,"endDate":null,"aprPercent":null,
                           "settlementPence":null,"settlementAsOf":null,"nextPaymentDate":null,"paymentsRemaining":null,
                           "originalPrincipalPence":null,"rateNote":"","notes":"","sourceIds":[]},
                          {"id":"loan-2","lender":"Example Credit","purpose":"Laptop","status":"repaid",
                           "monthlyPaymentPence":5000,"userSharePence":5000,"balancePence":0,"balanceAsOf":"2026-09-01",
                           "balanceKind":"statement","startDate":"2025-01-01","endDate":"2026-09-01","aprPercent":9.9,
                           "settlementPence":0,"settlementAsOf":"2026-09-01","nextPaymentDate":"2026-10-01",
                           "paymentsRemaining":0,"originalPrincipalPence":60000,"rateNote":"Fixed","notes":"",
                           "sourceIds":["statement-1"]}],
                 "notes":["Example note"],
                 "sources":[{"id":"statement-1","label":"Example statement","url":"https://example.com/statement",
                             "asOf":"2026-09-01"}]}""");
    }

    /** A complete equity position; its first grant knows nothing optional, its second everything. */
    public static ObjectNode positionDraft(String company) {
        var draft = (ObjectNode) JSON.readTree("""
                {"company":"Example Co","asOf":"2026-09-20","ownedShares":100,
                 "stockPlan":{"summary":"Hold","status":"tentative","waitingFor":"","nextAction":"Review","reviewMonth":null},
                 "optionPlan":{"summary":"Wait","status":"undecided","waitingFor":"Vest","nextAction":"","reviewMonth":"2027-01"},
                 "employmentNote":"Example employment note",
                 "grants":[{"id":"grant-1","grantDate":"2024-01-15","vested":50,"unvested":150,"strikeUsd":1.25,
                            "originalExpiry":"2034-01-15","postEmploymentExpiry":null,"nextVest":null},
                           {"id":"grant-2","grantDate":"2025-01-15","vested":0,"unvested":100,"strikeUsd":2.5,
                            "originalExpiry":"2035-01-15","postEmploymentExpiry":"2027-01-15",
                            "nextVest":{"date":"2026-12-01","alternateDate":null,"quantity":25,"condition":"Still employed"}}],
                 "actions":[{"id":"action-1","title":"Read the plan rules","timing":"This month","dueDate":null,"done":false}],
                 "details":[{"id":"detail-1","title":"Example detail","body":"Fake notes"}],
                 "sources":[{"id":"source-1","label":"Example plan","url":"https://example.com/plan","asOf":"2026-09-01"}],
                 "scenario":{"pricesUsd":[5,10],"withholdingRate":0.45,"usdToGbp":0.75,"asOf":"2026-09-20","notes":""}}""");
        draft.put("company", company);
        return draft;
    }

    public static ObjectNode budgetForecast() {
        return (ObjectNode) JSON.readTree("""
                {"baselineMonth":"2026-09","items":[
                 {"id":"rent","label":"Example rent","amountPence":100000,"group":"non-discretionary"},
                 {"id":"groceries","label":"Example groceries","amountPence":30000,"group":"necessary"},
                 {"id":"streaming","label":"Example streaming","amountPence":1500,"group":"discretionary"}]}""");
    }

    public static ObjectNode spendingDetailsDraft() {
        var draft = reviewDraft();
        draft.set("budgetForecast", budgetForecast().put("incomePence", 250000)
                .put("incomeBasis", "Example usual net salary estimate from the dated review."));
        draft.set("transactions", JSON.readTree("""
                [
                 {
                  "id":"example-household-transfer",
                  "accountId":"example-current",
                  "month":"2026-08",
                  "merchant":"Example household transfer",
                  "description":"User-confirmed monthly transfer; exact day not supplied.",
                  "category":"House bills",
                  "amountPence":100000,
                  "origin":"user"
                 },
                 {
                  "id":"example-card-payment",
                  "accountId":"example-current",
                  "date":"2026-08-15",
                  "month":"2026-08",
                  "merchant":"Example Market",
                  "description":"Card payment",
                  "category":"Groceries",
                  "amountPence":4200,
                  "detail":"Example card payment reference.",
                  "recurrence":"one-off",
                  "sourceIds":[
                   "statement-1"
                  ],
                  "origin":"bank"
                 },
                 {
                  "id":"example-card-refund",
                  "accountId":"example-current",
                  "date":"2026-08-16",
                  "month":"2026-08",
                  "merchant":"Example Market",
                  "description":"Refund",
                  "category":"Groceries",
                  "amountPence":-2000,
                  "recurrence":"unknown",
                  "sourceIds":[
                   "statement-1"
                  ],
                  "origin":"bank"
                 }
                ]
                """));
        return draft;
    }

    public static ObjectNode loanProfile(String id, boolean isDefault) {
        var profile = (ObjectNode) JSON.readTree("""
                {"id":"example-profile","name":"Example extra payments","isDefault":false,
                 "firstPaymentMonth":"2026-10","selectedLoanId":null,"showShare":false,"reviewAsOf":"2026-09-20",
                 "loans":[{"loanId":"loan-1","balancePence":1000000,"annualRatePercent":12.0,
                 "monthlyPaymentPence":20000,"monthlyExtraPence":10000,"lumpSumPence":0,
                 "extraStartsAfterMonths":0,"sharePercent":50.0,"matchExtra":false,
                 "repaymentMode":"shorten-term","balanceAsOf":null}]}""");
        profile.put("id", id);
        profile.put("isDefault", isDefault);
        return profile;
    }

    public static String profileChange(JsonNode profile, String expectedUpdatedAt) {
        var body = JSON.createObjectNode();
        body.putObject("loanProfileChange").put("operation", "save").set("profile", profile);
        body.put("expectedUpdatedAt", expectedUpdatedAt);
        return JSON.writeValueAsString(body);
    }

    public static String saveReview(JsonNode draft, String expectedUpdatedAt) {
        return envelope("review", draft, expectedUpdatedAt);
    }

    public static String savePosition(JsonNode draft, String expectedUpdatedAt) {
        return envelope("position", draft, expectedUpdatedAt);
    }

    public static JsonNode read(String body) {
        return JSON.readTree(body);
    }

    private static String envelope(String name, JsonNode draft, String expectedUpdatedAt) {
        var body = JSON.createObjectNode();
        body.set(name, draft);
        body.put("expectedUpdatedAt", expectedUpdatedAt);
        return JSON.writeValueAsString(body);
    }
}
