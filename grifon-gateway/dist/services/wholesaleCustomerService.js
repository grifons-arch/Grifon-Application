"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listWholesaleCustomers = void 0;
const customerService_1 = require("./customerService");
const toWholesaleCustomer = (customer) => ({
    customerId: customer.customerId,
    shopId: customer.shopId,
    email: customer.email,
    firstName: customer.firstName,
    lastName: customer.lastName,
    company: customer.company,
    active: customer.active,
    defaultGroupId: customer.defaultGroupId,
    defaultGroupName: customer.defaultGroupName,
    wholesaleGroupIds: customer.wholesaleGroupIds,
    wholesaleGroupNames: customer.wholesaleGroupNames
});
const listWholesaleCustomers = async (client, shopId, lang) => {
    const customers = await (0, customerService_1.listCustomers)(client, shopId, lang);
    return customers.filter((customer) => customer.isWholesale).map(toWholesaleCustomer);
};
exports.listWholesaleCustomers = listWholesaleCustomers;
