/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id$

package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyClass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.AccessTypeInformation;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * Class used to collect meta information about an annotated entity.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	protected final ImportContext importContext;
	protected final TypeElement element;
	protected final Map<String, MetaAttribute> members;
	protected Context context;

	private AccessTypeInformation entityAccessTypeInfo;

	public AnnotationMetaEntity(TypeElement element, Context context) {
		this( element, context, false );
	}

	protected AnnotationMetaEntity(TypeElement element, Context context, boolean lazilyInitialised) {
		this.element = element;
		this.context = context;
		this.members = new HashMap<String, MetaAttribute>();
		this.importContext = new ImportContextImpl( getPackageName() );
		if ( !lazilyInitialised ) {
			init();
		}
	}

	public Context getContext() {
		return context;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = context.getElementUtils().getPackageOf( element );
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		return new ArrayList<MetaAttribute>( members.values() );
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	public void mergeInMembers(Collection<MetaAttribute> attributes) {
		for ( MetaAttribute attribute : attributes ) {
			members.put( attribute.getPropertyName(), attribute );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( ", members=" ).append( members );
		sb.append( '}' );
		return sb.toString();
	}
	
	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = TypeUtils.determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( TypeUtils.containsAnnotation( memberOfClass, Transient.class )
					|| memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
					|| memberOfClass.getModifiers().contains( Modifier.STATIC ) ) {
				continue;
			}

			TypeVisitor visitor = new TypeVisitor( this );
			AnnotationMetaAttribute result = memberOfClass.asType().accept( visitor, memberOfClass );
			if ( result != null ) {
				members.put( result.getPropertyName(), result );
			}
		}
	}

	protected void init() {
		TypeUtils.determineAccessTypeForHierarchy( element, context );
		entityAccessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		addPersistentMembers( methodsOfClass, AccessType.PROPERTY );
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	class TypeVisitor extends SimpleTypeVisitor6<AnnotationMetaAttribute, Element> {

		/**
		 * FQCN of the Hibernate specific @Target annotation. We do not use the class directly to avoid depending on Hibernate
		 * Core.
		 */
		private static final String ORG_HIBERNATE_ANNOTATIONS_TARGET = "org.hibernate.annotations.Target";

		AnnotationMetaEntity parent;

		TypeVisitor(AnnotationMetaEntity parent) {
			this.parent = parent;
		}

		@Override
		public AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
			return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
		}

		@Override
		public AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
			// METAGEN-2 - For now we handle arrays as SingularAttribute
			// The code below is an attempt to be closer to the spec and only allow byte[], Byte[], char[] and Character[]
//			AnnotationMetaSingleAttribute attribute = null;
//			TypeMirror componentMirror = t.getComponentType();
//			if ( TypeKind.CHAR.equals( componentMirror.getKind() )
//					|| TypeKind.BYTE.equals( componentMirror.getKind() ) ) {
//				attribute = new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
//			}
//			else if ( TypeKind.DECLARED.equals( componentMirror.getKind() ) ) {
//				TypeElement componentElement = ( TypeElement ) context.getProcessingEnvironment()
//						.getTypeUtils()
//						.asElement( componentMirror );
//				if ( BASIC_ARRAY_TYPES.contains( componentElement.getQualifiedName().toString() ) ) {
//					attribute = new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
//				}
//			}
//			return attribute;
			return new AnnotationMetaSingleAttribute( parent, element, TypeUtils.toTypeString( t ) );
		}

		public AnnotationMetaAttribute visitTypeVariable(TypeVariable t, Element element) {
			// METAGEN-29 - for a type variable we use the upper bound
			TypeMirror mirror = t.getUpperBound();
			TypeMirror erasedType = context.getTypeUtils().erasure( mirror );
			return new AnnotationMetaSingleAttribute(
					parent, element, erasedType.toString()
			);
		}

		@Override
		public AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
			AnnotationMetaAttribute metaAttribute = null;
			TypeElement returnedElement = ( TypeElement ) context.getTypeUtils().asElement( declaredType );
			// WARNING: .toString() is necessary here since Name equals does not compare to String
			String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
			String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
			if ( collection != null ) {
				return createMetaCollectionAttribute(
						declaredType, element, fqNameOfReturnType, collection, targetEntity
				);
			}
			else if ( isBasicAttribute( element, returnedElement ) ) {
				String type = targetEntity != null ? targetEntity : returnedElement.getQualifiedName().toString();
				return new AnnotationMetaSingleAttribute( parent, element, type );
			}
			return metaAttribute;
		}

		private AnnotationMetaAttribute createMetaCollectionAttribute(DeclaredType declaredType, Element element, String fqNameOfReturnType, String collection, String targetEntity) {
			if ( TypeUtils.containsAnnotation( element, ElementCollection.class ) ) {
				String explicitTargetEntity = getTargetEntity( element.getAnnotationMirrors() );
				TypeMirror collectionElementType = TypeUtils.getCollectionElementType(
						declaredType, fqNameOfReturnType, explicitTargetEntity, context
				);
				final TypeElement collectionElement = ( TypeElement ) context.getTypeUtils()
						.asElement( collectionElementType );
				AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( collectionElement.getQualifiedName().toString() );
				if ( accessTypeInfo == null ) {
					AccessType explicitAccessType = TypeUtils.determineAnnotationSpecifiedAccessType(
							collectionElement
					);
					accessTypeInfo = new AccessTypeInformation(
							collectionElement.getQualifiedName().toString(),
							explicitAccessType,
							entityAccessTypeInfo.getAccessType()
					);
					context.addAccessTypeInformation(
							collectionElement.getQualifiedName().toString(), accessTypeInfo
					);
				}
				else {
					accessTypeInfo.setDefaultAccessType( entityAccessTypeInfo.getAccessType() );
				}
			}
			if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
				return createAnnotationMetaAttributeForMap( declaredType, element, collection, targetEntity );
			}
			else {
				return new AnnotationMetaCollection(
						parent, element, collection, getElementType( declaredType, targetEntity )
				);
			}
		}

		@Override
		public AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
			if ( !p.getKind().equals( ElementKind.METHOD ) ) {
				return null;
			}

			String string = p.getSimpleName().toString();
			if ( !StringUtil.isPropertyName( string ) ) {
				return null;
			}

			TypeMirror returnType = t.getReturnType();
			return returnType.accept( this, p );
		}

		private boolean isBasicAttribute(Element element, Element returnedElement) {
			if ( TypeUtils.containsAnnotation( element, Basic.class )
					|| TypeUtils.containsAnnotation( element, OneToOne.class )
					|| TypeUtils.containsAnnotation( element, ManyToOne.class ) ) {
				return true;
			}

			BasicAttributeVisitor basicVisitor = new BasicAttributeVisitor();
			return returnedElement.asType().accept( basicVisitor, returnedElement );
		}

		private AnnotationMetaAttribute createAnnotationMetaAttributeForMap(DeclaredType declaredType, Element element, String collection, String targetEntity) {
			String keyType;
			if ( TypeUtils.containsAnnotation( element, MapKeyClass.class ) ) {
				TypeMirror typeMirror = ( TypeMirror ) TypeUtils.getAnnotationValue(
						TypeUtils.getAnnotationMirror(
								element, MapKeyClass.class
						), TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME
				);
				keyType = typeMirror.toString();
			}
			else {
				keyType = TypeUtils.getKeyType( declaredType, context );
			}
			return new AnnotationMetaMap(
					parent,
					element,
					collection,
					keyType,
					getElementType( declaredType, targetEntity )
			);
		}

		private String getElementType(DeclaredType declaredType, String targetEntity) {
			if ( targetEntity != null ) {
				return targetEntity;
			}
			final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
			if ( mirrors.size() == 1 ) {
				final TypeMirror type = mirrors.get( 0 );
				return TypeUtils.extractClosestRealTypeAsString( type, context );
			}
			else if ( mirrors.size() == 2 ) {
				return TypeUtils.extractClosestRealTypeAsString( mirrors.get( 1 ), context );
			}
			else {
				//for 0 or many
				//0 is expected, many is not
				if ( mirrors.size() > 2 ) {
					context.logMessage(
							Diagnostic.Kind.WARNING, "Unable to find the closest solid type" + declaredType
					);
				}
				return "?";
			}
		}

		/**
		 * @param annotations list of annotation mirrors.
		 *
		 * @return target entity class name as string or {@code null} if no targetEntity is here or if equals to void
		 */
		private String getTargetEntity(List<? extends AnnotationMirror> annotations) {
			String fullyQualifiedTargetEntityName = null;
			for ( AnnotationMirror mirror : annotations ) {
				if ( TypeUtils.isAnnotationMirrorOfType( mirror, ElementCollection.class ) ) {
					fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetClass" );
				}
				else if ( TypeUtils.isAnnotationMirrorOfType( mirror, OneToMany.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, ManyToMany.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, ManyToOne.class )
						|| TypeUtils.isAnnotationMirrorOfType( mirror, OneToOne.class ) ) {
					fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetEntity" );
				}
				else if ( TypeUtils.isAnnotationMirrorOfType( mirror, ORG_HIBERNATE_ANNOTATIONS_TARGET ) ) {
					fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "value" );
				}
			}
			return fullyQualifiedTargetEntityName;
		}

		private String getFullyQualifiedClassNameOfTargetEntity(AnnotationMirror mirror, String parameterName) {
			assert mirror != null;
			assert parameterName != null;

			String targetEntityName = null;
			Object parameterValue = TypeUtils.getAnnotationValue( mirror, parameterName );
			if ( parameterValue != null ) {
				TypeMirror parameterType = ( TypeMirror ) parameterValue;
				if ( !parameterType.getKind().equals( TypeKind.VOID ) ) {
					targetEntityName = parameterType.toString();
				}
			}
			return targetEntityName;
		}
	}

	/**
	 * Checks whether the visited type is a basic attribute according to the JPA 2 spec
	 * ( section 2.8 Mapping Defaults for Non-Relationship Fields or Properties)
	 */
	class BasicAttributeVisitor extends SimpleTypeVisitor6<Boolean, Element> {
		@Override
		public Boolean visitPrimitive(PrimitiveType t, Element element) {
			return Boolean.TRUE;
		}

		@Override
		public Boolean visitArray(ArrayType t, Element element) {
			TypeMirror componentMirror = t.getComponentType();
			TypeElement componentElement = ( TypeElement ) context.getTypeUtils().asElement( componentMirror );

			return Constants.BASIC_ARRAY_TYPES.contains( componentElement.getQualifiedName().toString() );
		}

		@Override
		public Boolean visitDeclared(DeclaredType declaredType, Element element) {
			if ( ElementKind.ENUM.equals( element.getKind() ) ) {
				return Boolean.TRUE;
			}

			if ( ElementKind.CLASS.equals( element.getKind() ) || ElementKind.INTERFACE.equals( element.getKind() ) ) {
				TypeElement typeElement = ( ( TypeElement ) element );
				String typeName = typeElement.getQualifiedName().toString();
				if ( Constants.BASIC_TYPES.contains( typeName ) ) {
					return Boolean.TRUE;
				}
				if ( TypeUtils.containsAnnotation( element, Embeddable.class ) ) {
					return Boolean.TRUE;
				}
				for ( TypeMirror mirror : typeElement.getInterfaces() ) {
					TypeElement interfaceElement = ( TypeElement ) context.getTypeUtils().asElement( mirror );
					if ( "java.io.Serializable".equals( interfaceElement.getQualifiedName().toString() ) ) {
						return Boolean.TRUE;
					}
				}
			}
			return Boolean.FALSE;
		}
	}
}
