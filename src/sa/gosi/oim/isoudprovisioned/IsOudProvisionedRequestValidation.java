package sa.gosi.oim.isoudprovisioned;

import java.util.List;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcLookupOperationsIntf;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.request.exception.InvalidRequestDataException;
import oracle.iam.request.plugins.RequestDataValidator;
import oracle.iam.request.vo.Beneficiary;
import oracle.iam.request.vo.RequestBeneficiaryEntity;
import oracle.iam.request.vo.RequestBeneficiaryEntityAttribute;
import oracle.iam.request.vo.RequestData;

public class IsOudProvisionedRequestValidation implements RequestDataValidator {

	public static final ODLLogger LOGGER = ODLLogger.getODLLogger("CUSTOM.EVENT.HANDLER");
	private static final String CLASS_NAME = null;
	private static final String GENERIC_PROPS_LOOKUP = "Lookup.GOSI.Generic.Props";

	private String OUD_ACCOUNT_ERROR_MSG = null;
	private String OUD_DEFAULT_MSG = "The beneficiary of this request doesn't have OUD account. Please raise a request for OUD account before requesting for RASED access.";
	private tcLookupOperationsIntf lookupOprIntrface = null;
	@Override
	public void validate(RequestData requestData) throws InvalidRequestDataException {
		final String METHOD_NAME = "validate";
		LOGGER.info("Enter " + CLASS_NAME + "-" + METHOD_NAME);
		System.out.println(
				"============================================= Role request Beneficiary ==========================================");
		List<Beneficiary> beneficiaries = requestData.getBeneficiaries();
		List<RequestBeneficiaryEntity> beneficiaryEntities = null;
		List<RequestBeneficiaryEntityAttribute> beneficiaryEntityAttributes = null;
		RequestBeneficiaryEntity beneficiaryEntityGlobal = null;
		String usrKey = "";
		String lookupName = "Lookup.Rased.Role.Requestable";
		String appInstanceName = "OUDUser";
		String DecodeRoleValue = null;
		String entityOperation = null;
		for (Beneficiary beneficiary : beneficiaries) {
			System.out.println("request Beneficiary --->" + beneficiary);
			beneficiaryEntities = beneficiary.getTargetEntities();
			// Get user key
			usrKey = beneficiary.getBeneficiaryKey();
			System.out.println("Beneficiary Key (usr_key) ---> " + usrKey);
			LOGGER.info("Beneficiary Key (usr_key) ---> " + usrKey);
			System.out.println("Request Beneficiary Entities --->" + beneficiaryEntities);
			for (RequestBeneficiaryEntity beneficiaryEntity : beneficiaryEntities) {
				System.out.println("request Beneficiary Entity --->" + beneficiaryEntity);
				beneficiaryEntityGlobal = beneficiaryEntity;
				entityOperation = beneficiaryEntity.getOperation();
				String entityType = beneficiaryEntity.getEntityType();
				String entitySubType = beneficiaryEntity.getEntitySubType();
				System.out.println("=====\nEntity Type :" + entityType + " , Entity Sub Type " + entitySubType
						+ "Entity Operation : " + entityOperation + "\n=====");
				LOGGER.info("=====\nEntity Type :" + entityType + " , Entity Sub Type " + entitySubType
						+ "Entity Operation : " + entityOperation + "\n=====");
				// setting ecoded value by rececived by request id.
				// encodedVlaue = entitySubType;
				DecodeRoleValue = getDecodeRoleValue(lookupName, entitySubType.toUpperCase());
				System.out.println("Role from lookup : " + DecodeRoleValue);
				if (DecodeRoleValue != null) {
					if (DecodeRoleValue.equals(entitySubType.toUpperCase())) {
						System.out.println(
								"Role requested :" + DecodeRoleValue + ", Checking user has OUD Account or Not!");
						Boolean isOudProvisionedTouser = doesUserHasTheOUDAccount(usrKey, appInstanceName);
						if (isOudProvisionedTouser) {
							System.out.println(
									"User has the OUD account, Allow to request " + entitySubType + " to user.");
						} else {
							if (OUD_ACCOUNT_ERROR_MSG != null) {
								throw new InvalidRequestDataException(OUD_ACCOUNT_ERROR_MSG);
							} else {
								throw new InvalidRequestDataException(OUD_DEFAULT_MSG);
							}
						}
					} else {
						LOGGER.info("Unknown role requested,Please check the requested role and role present in "
								+ lookupName);
						System.out.println("Unknown role requested,Please check the requested role and role present in "
								+ lookupName);
					}
				} else {
					LOGGER.info("Role is not in RASED lookup, Skipping validation for this role : " + entitySubType);
					System.out.println(
							"Role is not in RASED lookup, Skipping validation for this role : " + entitySubType);
				}
			}
		}
	}
	private String getDecodeRoleValue(String lookupName, String encodeRoleName) {
		tcLookupOperationsIntf lookupOprIntrface = (tcLookupOperationsIntf) Platform
				.getService(tcLookupOperationsIntf.class);
		String METHOD_NAME = "getDecodeRoleValue:";
		LOGGER.info(CLASS_NAME + METHOD_NAME + "Getting Role name from lookup for RASED, Lookup name : " + lookupName
				+ " : Searching Role name in lookup : " + encodeRoleName);
		System.out.println("Searching role name in lookup : " + lookupName + " and value is " + encodeRoleName);
		String decodedRoleName = null;
		try {
			decodedRoleName = lookupOprIntrface.getDecodedValueForEncodedValue(lookupName, encodeRoleName);
			System.out.println("Decoded Role Name from lookup : " + decodedRoleName);
			if (decodedRoleName == null || decodedRoleName.equals("") || decodedRoleName.trim().equals("")) {
				decodedRoleName = null;
			}
		} catch (tcAPIException e) {
			e.printStackTrace();
		}
		return decodedRoleName;
	}

	private boolean doesUserHasTheOUDAccount(String userKey, String applicationInstanceName) {
		String METHOD_NAME = "::doesUserHasTheAccount::";
		System.out.println("Checking OUD account for user : " + userKey);
		boolean condition = false;
		// Intialazing lookup service
		lookupOprIntrface = (tcLookupOperationsIntf) Platform.getService(tcLookupOperationsIntf.class);
		UserManager userManagerService = (UserManager) Platform.getService(UserManager.class);
		try {
			LOGGER.info(CLASS_NAME + METHOD_NAME + "user is : " + userKey);
			ProvisioningService ps = (ProvisioningService) Platform.getService(ProvisioningService.class);
			String accountId = "";
			ApplicationInstanceService ais = (ApplicationInstanceService) Platform
					.getService(ApplicationInstanceService.class);
			ApplicationInstance DBUMAppInstance = ais.findApplicationInstanceByName(applicationInstanceName);
			SearchCriteria acccriteria = new SearchCriteria(
					ProvisioningConstants.AccountSearchAttribute.APPINST_KEY.getId(),
					Long.valueOf(DBUMAppInstance.getApplicationInstanceKey()), SearchCriteria.Operator.EQUAL);
			for (Account per : ps.getAccountsProvisionedToUser(userKey, acccriteria, null, true)) {
				String fetchedAccountName = per.getAccountDescriptiveField();
				String fetchedAccountStatus = per.getAccountStatus();
				String fetchedAccountType = per.getAccountType().toString();
				if (((fetchedAccountStatus != null) && ("provisioned".equalsIgnoreCase(fetchedAccountStatus)))
						|| ((fetchedAccountStatus != null) && ("enabled".equalsIgnoreCase(fetchedAccountStatus)))) {
					condition = true;
					LOGGER.info(CLASS_NAME + METHOD_NAME + "account name is : " + fetchedAccountName + " : "
							+ per.getAccountStatus() + " : " + per.getAccountType());
					System.out.println(CLASS_NAME + METHOD_NAME + "account name is : " + fetchedAccountName + " : "
							+ per.getAccountStatus() + " : " + per.getAccountType());
					break;
				} else if (((fetchedAccountStatus != null) && ("disabled".equalsIgnoreCase(fetchedAccountStatus)))) {
					condition = false;
					System.out.println("OUD Account for this user has been disabled, Return false");
					OUD_ACCOUNT_ERROR_MSG = lookupOprIntrface.getDecodedValueForEncodedValue(GENERIC_PROPS_LOOKUP,
							"OUD_ACCOUNT_DISABLE_MSG");
				} else if (((fetchedAccountStatus != null) && ("revoked".equalsIgnoreCase(fetchedAccountStatus)))) {
					condition = false;
					System.out.println("OUD Account for this user has been revoked, Return false");
					OUD_ACCOUNT_ERROR_MSG = lookupOprIntrface.getDecodedValueForEncodedValue(GENERIC_PROPS_LOOKUP,
							"OUD_ACCOUNT_REVOKE_MSG");
				} else {
					OUD_ACCOUNT_ERROR_MSG = lookupOprIntrface.getDecodedValueForEncodedValue(GENERIC_PROPS_LOOKUP,
							" OUD_ACCOUNT_NOT_PROVISIONED_MSG");
					System.out.println("User does not have OUD account, Return false");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return condition;
	}
}
