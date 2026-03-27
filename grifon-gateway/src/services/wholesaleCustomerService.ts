import { PrestaShopClient } from "../clients/PrestaShopClient";
import { listCustomers, CustomerItem } from "./customerService";

export interface WholesaleCustomerItem {
  customerId: number;
  shopId: number;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  company: string | null;
  active: boolean;
  defaultGroupId: number | null;
  defaultGroupName: string | null;
  wholesaleGroupIds: number[];
  wholesaleGroupNames: string[];
}

const toWholesaleCustomer = (customer: CustomerItem): WholesaleCustomerItem => ({
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

export const listWholesaleCustomers = async (
  client: PrestaShopClient,
  shopId: number,
  lang?: number
): Promise<WholesaleCustomerItem[]> => {
  const customers = await listCustomers(client, shopId, lang);
  return customers.filter((customer) => customer.isWholesale).map(toWholesaleCustomer);
};
