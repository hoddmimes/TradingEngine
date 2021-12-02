package com.hoddmimes.te.marketdata;

import com.hoddmimes.te.messages.EngineBdxInterface;

import java.util.*;

public class SubscriptionFilter {
	public static final char DELIMITER = '/';
	public static final String WILDCARD = "*";
	public static final String WILDREST = "...";

	enum NodeType {
		NORMAL, WILDCARD, WILDREST
	};

	KeyNode mRoot;
	HashMap<Object, KeyNode> mSubscriptionMap;

	public SubscriptionFilter() {
		mRoot = new KeyNode("ROOT");
		mSubscriptionMap = new HashMap<Object, KeyNode>();
	}

	public int getActiveSubscriptions() {
		return mRoot.countActiveSubscriptions();
	}


	public Object add(String pSubjectName, SubscriptionUpdateCallbackIf pCallback, Object pCallbackParameter) throws Exception {
		SubjectTokenParser tKeys = new SubjectTokenParser(pSubjectName);
		if (!tKeys.hasMore()) {
			throw new Exception("Invalid pSubjectName: " + pSubjectName);
		}

		KeyNode tKeyNode = mRoot;
		while (tKeys.hasMore()) {
			tKeyNode = tKeyNode.addChild(tKeys.getNextElement());
		}

		return tKeyNode.addSubscription(pSubjectName, pCallback,
				pCallbackParameter);
	}

	public void match(String pSubjectName, EngineBdxInterface pData) {
		SubjectTokenParser tKeys = new SubjectTokenParser(pSubjectName);
		mRoot.matchRecursive(pSubjectName, tKeys, pData );
	}

	public boolean matchAny(String pSubjectName) {
		SubjectTokenParser tKeys = new SubjectTokenParser(pSubjectName);
		return mRoot.matchAny(tKeys);
	}

	public List<String> getActiveSubscriptionsStrings() {
		List<String> tList = new ArrayList<>();
		return mRoot.getActiveSubscriptionsStrings("");
	}

	@Override
	public String toString() {
		StringBuffer tSB = new StringBuffer();
		mRoot.dumpSubscriptions(tSB, "");
		return tSB.toString();
	}

	public void remove(Object pHandle) {
		if (pHandle == null) {
			mRoot.removeAll();
			mSubscriptionMap.clear();
		} else {
			KeyNode tKeyNode = mSubscriptionMap.remove(pHandle);
			if ((tKeyNode != null) && (tKeyNode.mSubscriptions != null)) {
				tKeyNode.mSubscriptions.remove(pHandle);
				
			}
			tKeyNode = null;
		}
	}

	class Subscription {
		String mSubjectName;
		SubscriptionUpdateCallbackIf mCallback;
		Object mCallbackParameter;

		Subscription(String pSubjectName,
		             SubscriptionUpdateCallbackIf pCallback, Object pCallbackParameter) {
			mSubjectName = pSubjectName;
			mCallback = pCallback;
			mCallbackParameter = pCallbackParameter;
		}
	}
 
	private void test() {
		int i = 0;
		try {

			String pSubjectname = "/foo/...";
			add(pSubjectname, new SubscriberCallback(pSubjectname), null);
			
			String tUpdateSubjectName = "/foo/bar/fie";
			System.out.println("MatchAny: " + this.matchAny(tUpdateSubjectName));
			this.match(tUpdateSubjectName, null );
			
			return;
			
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SubscriptionFilter tFilter = new SubscriptionFilter();
		tFilter.test();
	}

	class SubscriberCallback implements SubscriptionUpdateCallbackIf {
		String mPattern;

		SubscriberCallback(String pPattern) {
			mPattern = pPattern;
		}

		public void distributorUpdate(String pSubjectName, EngineBdxInterface pData, Object pCallbackParameter ) {
			 System.out.println("SUBCRIBER Subject: [" + pSubjectName +  "] Matching Subscription: [" + mPattern + "]");
		}
	}

	/**
	 * The key node class hold subscription per level Each KeyNode instance
	 * represent a level in the name hiaracy
	 * 
	 * @author POBE
	 * 
	 */

	class KeyNode {
		String mKey;
		NodeType mType;

		HashMap<String, KeyNode> mChildren; // children keys in the name heiracy
		KeyNode mWildcardChild; // wildcard children
		KeyNode mWildcardRestChild; // wild card rest
		ArrayList<Subscription> mSubscriptions; // subscriptions onthis level

		KeyNode(String pKey) {
			mKey = pKey;
			mChildren = null;
			mWildcardChild = null;
			mWildcardRestChild = null;

			if (mKey.equals(WILDCARD)) {
				mType = NodeType.WILDCARD;
			} else if (mKey.equals(WILDREST)) {
				mType = NodeType.WILDREST;
			} else {
				mType = NodeType.NORMAL;
			}
		}

		KeyNode addChild(String pChildKey) throws Exception {
			if (mType.equals(NodeType.WILDREST)) {
				throw new Exception( "Can not not add child to a \"WildcardRest\" node");
			}

			KeyNode tKeyNode = null;

			if (pChildKey.equals(WILDCARD)) {
				if (mWildcardChild == null) {
					mWildcardChild = new KeyNode(pChildKey);
				}
				return mWildcardChild;
			} else if (pChildKey.equals(WILDREST)) {
				if (mWildcardRestChild == null) {
					mWildcardRestChild = new KeyNode(pChildKey);
				}
				return mWildcardRestChild;
			} else {
				if (mChildren == null) {
					mChildren = new HashMap<String, KeyNode>();
				}
				tKeyNode = mChildren.get(pChildKey);
				if (tKeyNode == null) {
					tKeyNode = new KeyNode(pChildKey);
					mChildren.put(pChildKey, tKeyNode);
				}
			}
			return tKeyNode;
		}

		Subscription addSubscription(String pSubjectName, SubscriptionUpdateCallbackIf pCallback, Object pCallbackParameter) {
			Subscription tSubscription = new Subscription(pSubjectName,
					pCallback, pCallbackParameter);
			if (mSubscriptions == null) {
				mSubscriptions = new ArrayList<Subscription>();
			}
			mSubscriptions.add(tSubscription);
			mSubscriptionMap.put(tSubscription, this);
			return tSubscription;
		}

		List<String> getActiveSubscriptionsStrings( String pPrefix) {
			List<String> tSubLst = new ArrayList<>();
			getActiveSubscriptionsStrings(tSubLst, pPrefix);
			return tSubLst;
		}

		private void getActiveSubscriptionsStrings( List<String> pSubLst, String  pPrefix) {

			if ((mSubscriptions != null) && (mSubscriptions.size() > 0)) {
				pSubLst.add("References: " + mSubscriptions.size() + " Topic: "
						+ pPrefix + "/" + mKey + "\n");
			}

			Iterator<KeyNode> tItr = null;

			if (mKey.equals("ROOT")) {
				if (mChildren != null) {
					tItr = mChildren.values().iterator();
					while (tItr.hasNext()) {
						tItr.next().getActiveSubscriptionsStrings(pSubLst, "");
					}
				}
				if (mWildcardChild != null) {
					mWildcardChild.getActiveSubscriptionsStrings(pSubLst, "");
				}
				if (mWildcardRestChild != null) {
					mWildcardRestChild.getActiveSubscriptionsStrings(pSubLst,
							"");
				}
			} else {
				if (mChildren != null) {
					tItr = mChildren.values().iterator();
					while (tItr.hasNext()) {
						tItr.next().getActiveSubscriptionsStrings(pSubLst,
								pPrefix + "/" + mKey);
					}
				}
				if (mWildcardChild != null) {
					mWildcardChild.getActiveSubscriptionsStrings(pSubLst,
							pPrefix + "/" + mKey);
				}
				if (mWildcardRestChild != null) {
					mWildcardRestChild.getActiveSubscriptionsStrings(pSubLst,
							pPrefix + "/" + mKey);
				}
			}
		}

		void dumpSubscriptions(StringBuffer pSB, String pPrefix) {
			if ((mSubscriptions != null) && (mSubscriptions.size() > 0)) {
				pSB.append("References: " + mSubscriptions.size() + " Topic: "
						+ pPrefix + "/" + mKey + "\n");
			}

			Iterator<KeyNode> tItr = null;

			if (mKey.equals("ROOT")) {
				if (mChildren != null) {
					tItr = mChildren.values().iterator();
					while (tItr.hasNext()) {
						tItr.next().dumpSubscriptions(pSB, "");
					}
				}
				if (mWildcardChild != null) {
					mWildcardChild.dumpSubscriptions(pSB, "");
				}
				if (mWildcardRestChild != null) {
					mWildcardRestChild.dumpSubscriptions(pSB, "");
				}
			} else {
				if (mChildren != null) {
					tItr = mChildren.values().iterator();
					while (tItr.hasNext()) {
						tItr.next()
								.dumpSubscriptions(pSB, pPrefix + "/" + mKey);
					}
				}
				if (mWildcardChild != null) {
					mWildcardChild.dumpSubscriptions(pSB, pPrefix + "/" + mKey);
				}
				if (mWildcardRestChild != null) {
					mWildcardRestChild.dumpSubscriptions(pSB, pPrefix + "/"
							+ mKey);
				}
			}
		}

		public void removeAll() {
			mKey = null;
			mSubscriptions = null;

			
			if (mWildcardChild != null) {
				mWildcardChild.removeAll();
			}
			if (mWildcardRestChild != null) {
				mWildcardRestChild.removeAll();
			}
			mWildcardChild = null;
			mWildcardRestChild = null;
			
			if (mChildren == null) {
				return;
			}
			
			Iterator<KeyNode> tItr = mChildren.values().iterator();
			while (tItr.hasNext()) {
				tItr.next().removeAll();
			}
			mChildren = null;

		}

		int countActiveSubscriptions() {
			int tCount = 0;
			if (mChildren != null) {
				Iterator<KeyNode> tItr = mChildren.values().iterator();
				while (tItr.hasNext()) {
					tCount += tItr.next().countActiveSubscriptions();
				}
			}
			if (mSubscriptions != null) {
				tCount += mSubscriptions.size();
			}
			if (mWildcardChild != null) {
				tCount += mWildcardChild.countActiveSubscriptions();
			}
			if (mWildcardRestChild != null) {
				tCount += mWildcardRestChild.countActiveSubscriptions();
			}

			return tCount;
		}

		boolean matchAny(SubjectTokenParser pKeys) {

			/**
			 * Traversed the whole key 
			 * look if there are any subscriber at this level
			 * if so return true  
			 */
			if (!pKeys.hasMore()) {
				if ((mSubscriptions != null) && (mSubscriptions.size() > 0)) {
					return true;
				} else {
					return false;
				}
			}

			/**
			 * Examine if there are any wildcard subscribers at this level if so return true
			 */
			if ((mWildcardRestChild != null) && (mWildcardRestChild.mSubscriptions.size() > 0)) {
			   return true;
			} 

			if (mWildcardChild != null) {
				pKeys.getNextElement();
				if (mWildcardChild.matchAny(pKeys)) {
					return true;
				}
			}
			

			if (mChildren != null) {
				KeyNode tKeyNode = mChildren.get(pKeys.getNextElement());
				if (tKeyNode != null) {
					return tKeyNode.matchAny(pKeys);
				}
			}

			return false;
		}

		void matchRecursive(String pSubjectName, SubjectTokenParser pKeys, EngineBdxInterface pData) {
			if (!pKeys.hasMore()) {
				if (mSubscriptions != null) {
					Iterator<Subscription> tItr = mSubscriptions.iterator();
					while (tItr.hasNext()) {
						Subscription tSubscription = tItr.next();
						tSubscription.mCallback.distributorUpdate(pSubjectName, pData, tSubscription.mCallbackParameter );
					}
				}
			} else {
				KeyNode tKeyNode = null;
				if (mChildren != null) {
					tKeyNode = mChildren.get(pKeys.getNextElement());
					if (tKeyNode != null) {
						tKeyNode.matchRecursive(pSubjectName, pKeys,  pData );
					}
				}
				if (mWildcardChild != null) {
					pKeys.getNextElement();
					mWildcardChild.matchRecursive(pSubjectName, pKeys, pData );
				}
			}

			if (mWildcardRestChild != null) {
				Iterator<Subscription> tItr = mWildcardRestChild.mSubscriptions.iterator();
				while (tItr.hasNext()) {
					Subscription tSubscription = tItr.next();
					tSubscription.mCallback.distributorUpdate(pSubjectName, pData, tSubscription.mCallbackParameter );
				}
			}

		}

	}

}
