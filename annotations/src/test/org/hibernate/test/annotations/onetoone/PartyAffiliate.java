//$Id$
package org.hibernate.test.annotations.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PartyAffiliate {
	@Id
	String partyId;

	@OneToOne(mappedBy="partyAffiliate")
	Party party;

	String affiliateName;
}
